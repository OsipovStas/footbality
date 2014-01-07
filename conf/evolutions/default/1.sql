# --- First database schema

# --- !Ups

CREATE TABLE teams (
    id serial NOT NULL,
    name varchar(255) NOT NULL,
    PRIMARY KEY(id)
);

CREATE TABLE matches (
    id serial NOT NULL,
    home_id int NOT NULL,
    away_id int NOT NULL,
    home_score smallint NOT NULL,
    away_score smallint NOT NULL,
    played date not null,
    PRIMARY KEY (id)
);

CREATE TABLE tasks (
    id serial NOT NULL,
    team varchar(255) NOT NULL,
    result smallint,
    since date,
    PRIMARY KEY (id)
);

CREATE TABLE taskQueries (
    id serial NOT NULL,
    task_id int,
    PRIMARY KEY (id)
);

# --- !Downs

drop table if exists teams;
drop table if exists matches;
drop table if exists tasks;
drop table if exists taskQueries;