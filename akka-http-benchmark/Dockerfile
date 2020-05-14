FROM openjdk:8
ENV APP_HOME /myproxy/
WORKDIR $APP_HOME

COPY build.gradle gradlew gradlew.bat $APP_HOME
COPY gradle $APP_HOME/gradle
# download dependencies
RUN ./gradlew check

COPY . .
EXPOSE 1080 1081
ENTRYPOINT [ "./gradlew", "run" ]
