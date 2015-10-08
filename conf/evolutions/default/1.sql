# --- !Ups

create table "EMAIL" ("EMAIL" VARCHAR PRIMARY KEY, "JOINDATE" TIMESTAMP NOT NULL)

# --- !Downs

drop table "EMAIL"
