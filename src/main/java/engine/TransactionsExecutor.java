package engine;

import command.CancelCommand;
import command.OrderCommand;
import command.QueryCommand;
import command.TransactionsCommand;
import entity.Account;
import entity.Order;
import entity.Position;
import entity.Transaction;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import utils.EntityManagement;
import utils.XMLResponseGenerator;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TransactionsExecutor {
    TransactionsCommand command;

    public TransactionsExecutor(TransactionsCommand command) {
        this.command = command;
    }

    public String execute() {
        try {
            Document responseDocument = XMLResponseGenerator.generateResponseDocument();
            EntityManager entityManager = EntityManagement.getEntityManager();

            Account account;
            try {
                entityManager.getTransaction().begin();
                account = entityManager.find(Account.class, Long.parseLong(command.getAccountId()), LockModeType.PESSIMISTIC_READ);
                entityManager.getTransaction().commit();
            } finally {
                entityManager.close();
            }

            if (account != null) {
                Long accountId = Long.parseLong(command.getAccountId());
                for (Object subCommand : command.getCommands()) {
                    if (subCommand instanceof OrderCommand) {
                        Element orderResponse = executeOrder((OrderCommand) subCommand, responseDocument);
                        responseDocument.getDocumentElement().appendChild(orderResponse);
                    } else if (subCommand instanceof CancelCommand) {
                        Element cancelResponse = executeCancel((CancelCommand) subCommand, accountId, responseDocument);
                        responseDocument.getDocumentElement().appendChild(cancelResponse);
                    } else if (subCommand instanceof QueryCommand) {
                        Element queryResponse = executeQuery((QueryCommand) subCommand, accountId, responseDocument);
                        responseDocument.getDocumentElement().appendChild(queryResponse);
                    }
                }
            }
            else {
                Element error = XMLResponseGenerator.generateErrorResponseWithId(responseDocument, command.getAccountId(), "Invalid Account ID");
                for (Object ignored : command.getCommands()) {
                    responseDocument.getDocumentElement().appendChild(error.cloneNode(true));
                }
            }
            return XMLResponseGenerator.convertToString(responseDocument);
        }
        catch (ParserConfigurationException | TransformerException e) {
            return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><results><error>Unexpected XML Parser Error</error></results>";
        }
        catch (NumberFormatException e) {
            return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><results><error>Disallowed message format</error></results>";
        }
    }

    private Element executeOrder(OrderCommand orderCommand, Document responseDocument) {
        EntityManager entityManager = EntityManagement.getEntityManager();
        try {
            entityManager.getTransaction().begin();
            Order newOrder = new Order(orderCommand);
            Account newOrderAccount = entityManager.find(Account.class, Long.parseLong(command.getAccountId()), LockModeType.PESSIMISTIC_WRITE);

            entityManager.persist(newOrder);
            newOrder.setAccount(newOrderAccount);

            if (newOrder.getAmount() >= 0) {
                double cost = newOrder.getAmount() * newOrder.getLimitPrice();
                if (newOrderAccount.getBalance() >= cost) {
                    newOrderAccount.setBalance(newOrderAccount.getBalance() - cost);
                }
                else {
                    entityManager.getTransaction().rollback();
                    return XMLResponseGenerator.generateOrderErrorResponse(responseDocument, orderCommand, "Rejected for insufficient funds.");
                }
            }
            else {
                Position position = newOrderAccount.getPositions().stream()
                        .filter(pos -> orderCommand.getSym().equals(pos.getSym()))
                        .findFirst()
                        .orElse(null);

                String errorMessage = null;
                if (position != null) {
                    entityManager.lock(position, LockModeType.PESSIMISTIC_WRITE);
                    if (position.getAmount() >= - orderCommand.getAmount()){
                        position.setAmount(position.getAmount() + orderCommand.getAmount());
                        entityManager.merge(position);
                    }
                    else {
                        errorMessage = "Rejected for insufficient shares.";
                    }
                }
                else {
                    errorMessage = "Rejected for nonexistent position.";
                }

                if (errorMessage != null) {
                    entityManager.getTransaction().rollback();
                    return XMLResponseGenerator.generateOrderErrorResponse(responseDocument, orderCommand, errorMessage);
                }
            }

            List<Order> matchingOrders = this.getMatchingOrders(entityManager, newOrder);
            for (Order order : matchingOrders) {
                int tradeAmount = Math.min(Math.abs(newOrder.getAmount()), Math.abs(order.getAmount()));
                newOrder.setAmount(newOrder.getAmount() + (newOrder.getAmount() >= 0 ? -tradeAmount : tradeAmount));
                order.setAmount(order.getAmount() + (order.getAmount() >= 0 ? -tradeAmount : tradeAmount));

                long createdAt = Instant.now().getEpochSecond();

                Transaction transactionForMatch = new Transaction(tradeAmount, order.getLimitPrice(), createdAt, order);
                order.addTransaction(transactionForMatch);
                entityManager.persist(transactionForMatch);

                Transaction transactionForNewOrder = new Transaction(tradeAmount, order.getLimitPrice(), createdAt, newOrder);
                order.addTransaction(transactionForNewOrder);
                entityManager.persist(transactionForNewOrder);

                accountChangeForNewOrder(entityManager, newOrder, order, newOrderAccount, orderCommand, tradeAmount);

                if (order.getAmount() == 0) {
                    order.setStatus(Order.Status.EXECUTED);
                }
                entityManager.merge(order);

                if (newOrder.getAmount() == 0) {
                    newOrder.setStatus(Order.Status.EXECUTED);
                    break;
                }
            }

            entityManager.getTransaction().commit();

            return XMLResponseGenerator.generateOpenedResponse(responseDocument, newOrder.getId(), orderCommand.getSym(), orderCommand.getAmount(), orderCommand.getLimit());
        }
        catch (Exception e) {
            e.printStackTrace();
            entityManager.getTransaction().rollback();
            return XMLResponseGenerator.generateErrorResponse(responseDocument, "Transaction Error");
        } finally {
            entityManager.close();
        }
    }

    private List<Order> getMatchingOrders(EntityManager entityManager, Order order) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Order> cq = cb.createQuery(Order.class);
        Root<Order> orderRoot = cq.from(Order.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(orderRoot.get("symbol"), order.getSymbol()));
        predicates.add(cb.equal(orderRoot.get("status"), Order.Status.OPEN));

        if (order.getAmount() >= 0) {
            predicates.add(cb.lessThanOrEqualTo(orderRoot.get("limitPrice"), order.getLimitPrice()));
            predicates.add(cb.lessThan(orderRoot.get("amount"), 0));
            cq.orderBy(cb.desc(orderRoot.get("limitPrice")), cb.asc(orderRoot.get("id")));
        } else {
            predicates.add(cb.greaterThanOrEqualTo(orderRoot.get("limitPrice"), order.getLimitPrice()));
            predicates.add(cb.greaterThan(orderRoot.get("amount"), 0));
            cq.orderBy(cb.desc(orderRoot.get("limitPrice")), cb.asc(orderRoot.get("id")));
        }

        cq.select(orderRoot).where(cb.and(predicates.toArray(new Predicate[0])));

        TypedQuery<Order> query = entityManager.createQuery(cq).setLockMode(LockModeType.PESSIMISTIC_WRITE);

        return query.getResultList();
    }

    private void accountChangeForNewOrder(EntityManager entityManager, Order newOrder, Order order, Account newOrderAccount, OrderCommand orderCommand, int tradeAmount) {
        Account orderAccount = order.getAccount();
        entityManager.lock(orderAccount, LockModeType.PESSIMISTIC_WRITE);
        double balanceChange = newOrder.getAmount() >= 0 ? order.getLimitPrice() * tradeAmount : -(order.getLimitPrice() * tradeAmount);
        orderAccount.setBalance(orderAccount.getBalance() + balanceChange);
        if (newOrder.getAmount() >= 0){
            addPositionAmount(entityManager, newOrderAccount, tradeAmount, orderCommand.getSym());

            if (order.getLimitPrice() < newOrder.getLimitPrice()){
                newOrderAccount.setBalance(newOrderAccount.getBalance() + (newOrder.getLimitPrice() - order.getLimitPrice()) * tradeAmount);
            }
        }
        entityManager.merge(orderAccount);
    }

    private Element executeCancel(CancelCommand cancelCommand, Long accountId, Document responseDocument) {
        EntityManager entityManager = EntityManagement.getEntityManager();
        try {
            Element response;
            entityManager.getTransaction().begin();
            Order order = entityManager.find(Order.class, Long.parseLong(cancelCommand.getId()), LockModeType.PESSIMISTIC_WRITE);

            if (order != null) {
                if (order.getStatus() == Order.Status.OPEN) {
                    Account orderAccount = order.getAccount();
                    entityManager.lock(orderAccount, LockModeType.PESSIMISTIC_WRITE);
                    if (Objects.equals(orderAccount.getId(), accountId)) {
                        if (order.getAmount() >= 0) {
                            orderAccount.setBalance(orderAccount.getBalance() + (order.getLimitPrice() * order.getAmount()));
                            entityManager.merge(orderAccount);
                        } else {
                            addPositionAmount(entityManager, orderAccount, -order.getAmount(), order.getSymbol());
                        }

                        order.cancel();
                        response = XMLResponseGenerator.generateStatusByOrder(responseDocument, order, true);
                    }
                    else {
                        response = XMLResponseGenerator.generateErrorResponse(responseDocument, "The order does not belong to the account");
                    }
                } else {
                    response = XMLResponseGenerator.generateErrorResponseWithId(responseDocument, cancelCommand.getId(), "Order closed.");
                }
            }
            else {
                response = XMLResponseGenerator.generateErrorResponseWithId(responseDocument, cancelCommand.getId(), "Order not exist.");
            }

            entityManager.getTransaction().commit();
            return response;
        }
        catch (Exception e) {
            e.printStackTrace();
            entityManager.getTransaction().rollback();
            return XMLResponseGenerator.generateErrorResponse(responseDocument, "Transaction Error");
        } finally {
            entityManager.close();
        }
    }

    private void addPositionAmount(EntityManager entityManager, Account orderAccount, int amount, String symbol) {
        Position existingPosition = orderAccount.getPositions().stream()
                .filter(pos -> symbol.equals(pos.getSym()))
                .findFirst()
                .orElse(null);

        if (existingPosition != null) {
            entityManager.lock(existingPosition, LockModeType.PESSIMISTIC_WRITE);
            existingPosition.setAmount(existingPosition.getAmount() +  amount);
            entityManager.merge(existingPosition);
        }
        else {
            Position newPosition = new Position();
            newPosition.setSym(symbol);
            newPosition.setAmount(amount);
            newPosition.addAccount(orderAccount);
            entityManager.persist(newPosition);
        }
    }

    private Element executeQuery(QueryCommand queryCommand, Long accountId, Document responseDocument) {
        EntityManager entityManager = EntityManagement.getEntityManager();
        try {
            Element response;
            entityManager.getTransaction().begin();
            Order order = entityManager.find(Order.class, Long.parseLong(queryCommand.getId()), LockModeType.PESSIMISTIC_READ);
            if (order != null) {
                if (Objects.equals(order.getAccount().getId(), accountId)){
                    response = XMLResponseGenerator.generateStatusByOrder(responseDocument, order, false);
                }
                else {
                    response = XMLResponseGenerator.generateErrorResponse(responseDocument, "The order does not belong to the account");
                }
            }
            else {
                response = XMLResponseGenerator.generateErrorResponseWithId(responseDocument, queryCommand.getId(), "Order not exist.");
            }

            entityManager.getTransaction().commit();
            return response;
        }
        catch (Exception e) {
            e.printStackTrace();
            entityManager.getTransaction().rollback();
            return XMLResponseGenerator.generateErrorResponse(responseDocument, "Transaction Error");
        } finally {
            entityManager.close();
        }
    }
}
