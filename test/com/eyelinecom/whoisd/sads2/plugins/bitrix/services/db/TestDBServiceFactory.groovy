package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db

import org.hibernate.Session
import org.hibernate.cfg.Configuration
import org.hibernate.envers.tools.hbm2ddl.EnversSchemaGenerator
import org.hibernate.tool.hbm2ddl.SchemaExport

/**
 * author: Artem Voronov
 */
class TestDBServiceFactory {

  protected static Properties hiberProps

  synchronized static DBService createDBService() throws IOException {
    if (hiberProps == null) {
      hiberProps = load("test_db.properties")
    }
    final DBService db = new DBService(hiberProps, "/com/eyelinecom/whoisd/sads2/plugins/bitrix/services/db/hibernate-model-test.cfg.xml")

    createEmptyDB(hiberProps)

    // Initial DB fill
    final Session s = db.openSession()
    try {
      s.beginTransaction()
      preFillDb(s)
      s.getTransaction().commit()

    } catch (RuntimeException e) {
      s.getTransaction().rollback()
      throw e

    } finally {
      s.close()
    }

    return db
  }

  protected static Properties load(String resource) {
    Properties props = new Properties()
    try {
      InputStream is = TestDBServiceFactory.class.getResourceAsStream(resource) as InputStream
      props.load(is)
    } catch (IOException e) {
      e.printStackTrace()
    }
    return props
  }

  protected static void createEmptyDB(Properties hibernateProperties) {
    Configuration config = new Configuration()
    config.configure("/com/eyelinecom/whoisd/sads2/plugins/bitrix/model/model.cfg.xml")
    config.configure("/com/eyelinecom/whoisd/sads2/plugins/bitrix/services/db/hibernate-model-test.cfg.xml")
    config.addProperties(hibernateProperties)

    SchemaExport export = new EnversSchemaGenerator(config).export()
    export.create(false, true)
  }

  private static void preFillDb(Session s) {
    //todo здесь можно описать операции инициализации тестовой БД
  }
}
