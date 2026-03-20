.PHONY: build run test clean docker-build docker-run

build:
	./gradlew build -x test

run:
	./gradlew run

test:
	./gradlew test

clean:
	./gradlew clean

docker-build:
	docker build -t trade-event-bus .

docker-run:
	docker run -p 8080:8080 trade-event-bus

docker-stop:
	docker stop $$(docker ps -q --filter ancestor=trade-event-bus)

lint:
	./gradlew ktlintCheck

jar:
	./gradlew jar
