FROM java:8
WORKDIR /tokenizerTester/
COPY ./* ./
RUN javac test/tokenizerTester.java