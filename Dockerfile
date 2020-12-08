FROM java:8
WORKDIR /tokenizerTester/
COPY ./* ./
RUN javac tokenizerTester.java