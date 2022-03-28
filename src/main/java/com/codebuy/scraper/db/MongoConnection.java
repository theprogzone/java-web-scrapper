package com.codebuy.scraper.db;

import com.mongodb.MongoClient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class MongoConnection {
    private String host;
    private Integer port;
    private String username;
    private String password;

    public MongoClient getMongoClient() {
        return new MongoClient(this.host, this.port);
    }
}
