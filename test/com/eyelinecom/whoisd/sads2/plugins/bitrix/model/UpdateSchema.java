package com.eyelinecom.whoisd.sads2.plugins.bitrix.model;

import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.Application;
import org.hibernate.cfg.Configuration;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;

import java.util.Properties;

/**
 * Cоздаёт скрипт для обновления метаданных в БД. Его потом нужно использовать в liquibase
 */
public class UpdateSchema {

  public static void main(String... args) {
    Properties props = new Properties();

    props.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQL5InnoDBDialect");
    props.setProperty("hibernate.connection.driver_class", "com.mysql.jdbc.Driver");
    props.setProperty("hibernate.connection.url", "jdbc:mysql://192.168.2.200:3306/sads_plugin_bitrix?useUnicode=true&amp;characterEncoding=UTF8");
    props.setProperty("hibernate.connection.username", "USER_NAME");
    props.setProperty("hibernate.connection.password", "PASSWORD");

    Configuration conf = new Configuration().configure(Application.class.getResource("../model.cfg.xml"));
    conf.addProperties(props);

    SchemaUpdate update = new SchemaUpdate(conf);
    update.setOutputFile(args.length > 0 ? args[0] : "update-schema.sql");
    update.setDelimiter(";");
    update.setFormat(true);
    update.execute(false, false);
  }

}