-- liquibase formatted sql
-- changeset voronov:1

  CREATE SCHEMA IF NOT EXISTS sads_plugin_bitrix
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

    create table applications (
        id integer not null auto_increment,
        access_token varchar(255) not null,
        bot_id integer not null,
        create_date datetime not null,
        deleted bit not null,
        domain varchar(255) not null,
        lang varchar(7) not null,
        last_modified datetime not null,
        refresh_token varchar(255) not null,
        primary key (id)
    ) ENGINE=InnoDB default CHARSET=utf8;

    create table chats (
        id integer not null auto_increment,
        chat_id integer not null,
        type varchar(255) not null,
        app_id integer not null,
        primary key (id)
    ) ENGINE=InnoDB default CHARSET=utf8;

    create table income_messages (
        id integer not null auto_increment,
        text varchar(1000) not null,
        queue_id integer not null,
        primary key (id)
    ) ENGINE=InnoDB default CHARSET=utf8;

    create table operators (
        id integer not null auto_increment,
        operator_id integer not null,
        app_id integer not null,
        primary key (id)
    ) ENGINE=InnoDB default CHARSET=utf8;

    create table queues (
        id integer not null auto_increment,
        back_page TEXT,
        create_date datetime not null,
        lang varchar(7) not null,
        last_modified datetime not null,
        protocol varchar(255) not null,
        service_id varchar(255) not null,
        type varchar(255) not null,
        app_id integer not null,
        operator_id integer,
        user_id integer not null,
        primary key (id)
    ) ENGINE=InnoDB default CHARSET=utf8;

    create table users (
        id integer not null auto_increment,
        user_id varchar(255) not null unique,
        primary key (id)
    ) ENGINE=InnoDB default CHARSET=utf8;

    alter table chats
        add constraint UK_67fd77ic4338jgxvbjlr3isx5  unique (app_id, chat_id, type);

    alter table operators
        add constraint UK_72wmvflvdyix8cdq47hxqnwr0  unique (app_id, operator_id);

    alter table queues
        add constraint UK_li121ekg9s1p936ockm6gpdu3  unique (app_id, user_id, service_id);

    alter table chats
        add constraint FK_clw5p27gq5qm7jjwsg91ge9gw
        foreign key (app_id)
        references applications (id);

    alter table income_messages
        add constraint FK_9t65s9vj9ain6bp2x1o1sqpq6
        foreign key (queue_id)
        references queues (id);

    alter table operators
        add constraint FK_j14ybef0p7vldl94ohibmyqrr
        foreign key (app_id)
        references applications (id);

    alter table queues
        add constraint FK_59xi76202wtekgvkpx7te00nl
        foreign key (app_id)
        references applications (id);

    alter table queues
        add constraint FK_tu41nblli0cwqf0x8hovu191
        foreign key (operator_id)
        references operators (id);

    alter table queues
        add constraint FK_cok038lal202a4giqy0x8r406
        foreign key (user_id)
        references users (id);

-- rollback ALTER TABLE queues DROP FOREIGN KEY FK_cok038lal202a4giqy0x8r406;
-- rollback ALTER TABLE queues DROP FOREIGN KEY FK_tu41nblli0cwqf0x8hovu191;
-- rollback ALTER TABLE queues DROP FOREIGN KEY FK_59xi76202wtekgvkpx7te00nl;
-- rollback ALTER TABLE operators DROP FOREIGN KEY FK_j14ybef0p7vldl94ohibmyqrr;
-- rollback ALTER TABLE income_messages DROP FOREIGN KEY FK_9t65s9vj9ain6bp2x1o1sqpq6;
-- rollback ALTER TABLE chats DROP FOREIGN KEY FK_clw5p27gq5qm7jjwsg91ge9gw;

-- rollback ALTER TABLE queues DROP INDEX UK_li121ekg9s1p936ockm6gpdu3;
-- rollback ALTER TABLE operators DROP INDEX UK_72wmvflvdyix8cdq47hxqnwr0;
-- rollback ALTER TABLE chats DROP INDEX UK_67fd77ic4338jgxvbjlr3isx5;

-- rollback DROP TABLE users;
-- rollback DROP TABLE queues;
-- rollback DROP TABLE operators;
-- rollback DROP TABLE income_messages;
-- rollback DROP TABLE chats;
-- rollback DROP TABLE applications;