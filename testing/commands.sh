sudo docker run --name db --cpuset-cpus="0-3" -e POSTGRES_DB=postgres -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -v data-volume:/var/lib/postgresql/data -d postgres

sudo docker run --name app --cpuset-cpus="0-3" -p 12345:12345 --link db:db -d erss-hwk4-ss1481-zm96_app

sudo docker stop $(sudo docker ps -aq)

sudo docker rm $(sudo docker ps -aq)

sudo docker volume rm $(sudo docker volume ls -q)