FROM openjdk:18

ENV SBT_VERSION 1.7.3

RUN curl -L -o sbt-$SBT_VERSION.tgz https://github.com/sbt/sbt/releases/download/v$SBT_VERSION/sbt-$SBT_VERSION.tgz
RUN tar -xf sbt-$SBT_VERSION.tgz

WORKDIR /CS441-CloudSimPlus-HW3

COPY . /CS441-CloudSimPlus-HW3

CMD /sbt/bin/sbt run