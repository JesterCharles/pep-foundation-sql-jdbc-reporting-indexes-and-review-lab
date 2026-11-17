# Local Run Tests image (Exa Studio builds this; the server grader does not).
# Warms ~/.m2 from pom.xml + a throwaway test so `mvn -o test` runs offline.
# Never copies src/, so it builds whatever state the learner's code is in.
FROM maven:3.9.9-eclipse-temurin-17
WORKDIR /warm
COPY pom.xml ./
RUN mkdir -p src/test/java/warm \
 && printf 'package warm;\nclass WarmTest { @org.junit.jupiter.api.Test void ok() {} }\n' \
      > src/test/java/warm/WarmTest.java \
 && mvn -B -q test \
 && rm -rf /warm
WORKDIR /work
