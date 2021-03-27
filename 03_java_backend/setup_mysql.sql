DROP DATABASE IF EXISTS crawl;
CREATE DATABASE crawl;
USE crawl;

CREATE TABLE `player` (
    `id` int unsigned AUTO_INCREMENT,
    `username` varchar(50) UNIQUE not null,
    `hashword` int unsigned not null,
    `email` varchar(65) not null,
    PRIMARY KEY (`id`)
) ENGINE=Aria;

CREATE TABLE `character` (
    `id` int unsigned AUTO_INCREMENT,
    `player_id` int unsigned,
    `dateCreated` datetime default CURRENT_TIMESTAMP,
    `lastSession` datetime default null ON UPDATE CURRENT_TIMESTAMP,
    `dateOfDeath` datetime default null ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=Aria;


