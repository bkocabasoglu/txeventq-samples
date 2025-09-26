FROM openjdk:23-jdk-slim

WORKDIR /app

# Install Maven
RUN apt-get update && apt-get install -y maven

# Copy everything
COPY . .

# Download dependencies and compile
RUN mvn dependency:copy-dependencies -DoutputDirectory=lib && mvn compile

# Default command (will be overridden)
CMD ["java", "-cp", "target/classes:lib/*", "com.oracle.osd.ClaimUpdatesConsumer"]
