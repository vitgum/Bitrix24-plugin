-- liquibase formatted sql
-- changeset voronov:1

  alter table income_messages add column image_url TEXT;
  alter table income_messages add column type varchar(255) not null;
  alter table income_messages modify text varchar(255);

-- rollback alter table income_messages modify text varchar(255) not null DEFAULT '...';
-- rollback alter table income_messages drop column type;
-- rollback alter table income_messages drop column image_url;