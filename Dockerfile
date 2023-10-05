FROM eclipse-temurin:17
ENV JASYPT_ENCRYPTOR_PASSWORD=NBAwweNFLnbl127134
ENV JWT_SECRET_SALT=TAOSportManagementForEveryBasketballClubs
ENV MONGO_CUSTOMER_PASSWORD=OzapDuyqF1GSFZPg
ENV MONGO_CUSTOMER_USER=taoElszamolas
ENV CUSTOMER_SUPER_USER=davidk
ENV CUSTOMER_SUPER_USER_PASSWORD=Songoku_1
RUN mkdir - p /app
WORKDIR /app
EXPOSE 1901
COPY sm-customer-services/target/sm-customer-services.jar ./
CMD ["java", "-jar", "sm-customer-services.jar"]