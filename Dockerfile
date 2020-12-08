FROM java:8
WORKDIR /tokenizerTester/
COPY ./* ./
RUN javac src/test/tokenizerTester.java