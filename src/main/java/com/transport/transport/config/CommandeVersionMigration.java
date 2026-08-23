package com.transport.transport.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import com.transport.transport.model.Commande;

/** Initialise le verrou optimiste sur les commandes creees avant son ajout. */
@Component
public class CommandeVersionMigration implements ApplicationRunner {
  private final MongoTemplate mongo;

  public CommandeVersionMigration(MongoTemplate mongo) {
    this.mongo = mongo;
  }

  @Override
  public void run(ApplicationArguments args) {
    mongo.updateMulti(Query.query(Criteria.where("version").exists(false)),
        new Update().set("version", 0L), Commande.class);
  }
}
