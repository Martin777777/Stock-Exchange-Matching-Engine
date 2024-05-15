package engine;

import command.*;
import entity.Account;
import entity.Position;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import utils.EntityManagement;
import utils.XMLResponseGenerator;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.util.List;

public class CreateExecutor {
    CreateCommand command;

    public CreateExecutor(CreateCommand command) {
        this.command = command;
    }

    public String execute() {
        try {
            Document responseDocument = XMLResponseGenerator.generateResponseDocument();
            List<Object> inputCommands = command.getCommands();
            for (Object command : inputCommands) {
                if (command instanceof CreateAccountCommand) {
                    CreateAccountCommand accountCommand = (CreateAccountCommand) command;
                    // handle accountCommand
                    handleAccountCommand(accountCommand, responseDocument);
                } else if (command instanceof SymbolCommand) {
                    // Handle SymbolCommand
                    SymbolCommand symbolCommand = (SymbolCommand) command;
                    // handle symbolCommand
                    handleSymbolCommand(symbolCommand, responseDocument);
                }
            }
            return XMLResponseGenerator.convertToString(responseDocument);
        }
        catch (ParserConfigurationException | TransformerException e) {
            return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><results><error>Unexpected XML Parser Error</error></results>";
        }
        catch (NumberFormatException e) {
            return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><error>Disallowed message format</error></results>";
        }
    }
    public void handleSymbolCommand(SymbolCommand symbolCommand, Document responseDocument) {
        String symbol = symbolCommand.getSym();
        List<SymbolAccountCommand> accounts = symbolCommand.getAccounts();

        //Handle the case where there are no accounts
        if (accounts == null || accounts.isEmpty()) {
            Element responseElement = XMLResponseGenerator.generateErrorResponse(responseDocument, "<error>No accounts specified for symbol: " + symbol + "</error>");
            responseDocument.getDocumentElement().appendChild(responseElement);
            return;
        }
        for (SymbolAccountCommand account : accounts) {
            insertNewShares(account, symbol, responseDocument);
        }
    }

    public void insertNewShares(SymbolAccountCommand account, String symbol, Document responseDocument) {
        // todo tidy up the code
        EntityManager entityManager = EntityManagement.getEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        long accountId = 0;
        try {
            transaction.begin();
            // Convert account ID from String to long
            //long accountId = Long.parseLong(account.getId());
            accountId = Long.parseLong(account.getId());
            // Attempt to find the target Account in the database
            Account targetAccount = entityManager.find(Account.class, accountId, LockModeType.PESSIMISTIC_WRITE);

            if (targetAccount == null) {
                // Target account does not exist, handle this case appropriately
                // throw new IllegalStateException("Account with ID " + accountId + " does not exist.");
                Element responseElement = XMLResponseGenerator.generateErrorCreateResponse(responseDocument, String.valueOf(accountId), symbol, "Account does not exist.");
                responseDocument.getDocumentElement().appendChild(responseElement);
                return;
            }
            Position existingPosition = findPositionBySymbol(targetAccount, symbol);
            // Position exists, update shares
            if (existingPosition != null) {
                entityManager.lock(existingPosition, LockModeType.PESSIMISTIC_WRITE);
                existingPosition.setAmount(existingPosition.getAmount() +  account.getShares());
                // Merge the entity
                entityManager.merge(existingPosition);
            }
            // No existing Position, proceed to persist a new Position
            else {
                Position newPosition = new Position();
                newPosition.setSym(symbol);
                newPosition.setAmount(account.getShares());
                newPosition.addAccount(targetAccount);
                entityManager.persist(newPosition);
            }
            Element responseElement = XMLResponseGenerator.generateCreatedResponse(responseDocument, accountId, symbol);
            responseDocument.getDocumentElement().appendChild(responseElement);
            // Commit if all operations are successful
            transaction.commit();
        } catch (NumberFormatException e) {
            // Handle case where account ID is not in a valid long format
            if (transaction.isActive()) {
                transaction.rollback();
            }
            //throw new IllegalArgumentException("Invalid account ID format: " + account.getId(), e);
            Element responseElement = XMLResponseGenerator.generateErrorCreateResponse(responseDocument, String.valueOf(accountId), symbol, "Invalid account ID format.");
            responseDocument.getDocumentElement().appendChild(responseElement);
        } catch (Exception e) {
            // Handle any other exceptions
            if (transaction.isActive()) {
                transaction.rollback();
            }
            //throw new RuntimeException("Failed to insert new shares due to an unexpected error.", e);
            Element responseElement = XMLResponseGenerator.generateErrorCreateResponse(responseDocument, String.valueOf(accountId), symbol, "Failed to insert new shares due to an unexpected error.");
            responseDocument.getDocumentElement().appendChild(responseElement);
        } finally {
            // Ensure the EntityManager is always closed
            if (entityManager.isOpen()) {
                entityManager.close();
            }
        }
    }

    private Position findPositionBySymbol(Account account, String symbol) {
        return account.getPositions().stream()
                .filter(pos -> symbol.equals(pos.getSym()))
                .findFirst()
                .orElse(null);
    }


    public void handleAccountCommand(CreateAccountCommand accountCommand, Document responseDocument) {
        // TODO add err handling or type check
        //Element responseElement = null;
        String accountID = accountCommand.getId();
        if (accountID == null || accountID.trim().isEmpty()) {
            //throw new IllegalArgumentException("Account ID cannot be null or empty.");
            Element responseElement = XMLResponseGenerator.generateErrorCreateResponse(responseDocument, accountID, null, "Account ID cannot be null or empty.");
            responseDocument.getDocumentElement().appendChild(responseElement);
            return;
        }
        double balance = accountCommand.getBalance();
        if (balance < 0) {
            //throw new IllegalArgumentException("Balance cannot be negative.");
            Element responseElement = XMLResponseGenerator.generateErrorCreateResponse(responseDocument, accountID, null, "Balance cannot be negative.");
            responseDocument.getDocumentElement().appendChild(responseElement);
            return;
        }
        // cast to long for persistent
        long accountIDLong;
        try {
            accountIDLong = Long.parseLong(accountID);
            responseDocument.getDocumentElement().appendChild(insertNewAcc(accountIDLong, balance, responseDocument));
        } catch (NumberFormatException e) {
            //throw new IllegalArgumentException("Account ID must be a valid long number.", e);
            Element responseElement = XMLResponseGenerator.generateErrorCreateResponse(responseDocument, accountID, null, "Account ID must be a valid long number.");
            responseDocument.getDocumentElement().appendChild(responseElement);
        }
    }



    public Element insertNewAcc(long accountIdLong, double balance, Document responseDocument) {
        EntityManager entityManager = EntityManagement.getEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            Account existedAccount = entityManager.find(Account.class, accountIdLong, LockModeType.PESSIMISTIC_WRITE);
            // Account does not exist
            if (existedAccount == null) {
                Account newAccount = new Account();
                newAccount.setId(accountIdLong);
                newAccount.setBalance(balance);
                entityManager.persist(newAccount);
                entityManager.getTransaction().commit();
                // Generate success response
                return XMLResponseGenerator.generateCreatedResponse(responseDocument, accountIdLong, null);
            } else {
                // Account already exists, error
                //System.out.println("Account with ID " + accountIdLong + " already exists.");
                transaction.rollback();
                return XMLResponseGenerator.generateErrorCreateResponse(responseDocument, String.valueOf(accountIdLong), null, "Account already exists.");
            }

            //entityManager.getTransaction().commit();

        } catch (PersistenceException e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            //throw new RuntimeException("Failed to insert new account due to persistence error", e);
            return XMLResponseGenerator.generateErrorCreateResponse(responseDocument, String.valueOf(accountIdLong), null, "Failed to insert new account due to persistence error.");
        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            //throw new RuntimeException("Failed to insert new account due to an unexpected error", e);
            return XMLResponseGenerator.generateErrorCreateResponse(responseDocument, String.valueOf(accountIdLong), null, "Failed to insert new account due to an unexpected error.");
        }
        finally {
            if (entityManager.isOpen()) {
                entityManager.close();
            }
        }
    }
}
