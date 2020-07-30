import appdynamics

import os
import time
import logging
import json
import pymysql
import rds_config
from random import seed
from random import randint
from random import uniform

logger = logging.getLogger()
logger.setLevel(logging.INFO)

rds_host  = "host_db_instance_endpoint"
name = rds_config.db_username
password = rds_config.db_password
db_name = rds_config.db_name

try:
    conn = pymysql.connect(rds_host, user=name, passwd=password, db=db_name, connect_timeout=5, cursorclass=pymysql.cursors.DictCursor)
except pymysql.MySQLError as e:
    logger.error("ERROR: Unexpected error: Could not connect to MySQL instance.")
    logger.error(e)
    sys.exit()

logger.info("SUCCESS: Connection to MySQL instance succeeded")

@appdynamics.tracer
def lambda_handler(event, context):    
    query = 'select key_field, field_2, some_other_field from Some_Table order by RAND() limit 1'

    retval = {}

    with conn.cursor() as cursor:
        with appdynamics.ExitCallContextManager(exit_point_type="DB", exit_point_sub_type="DB", identifying_properties={"VENDOR": "MYSQL", "HOST" : rds_host, "PORT" : "3306", "DATABASE" : db_name}) as ec:
            cursor.execute(query)
            retval = cursor.fetchone()

            if randint(1, 100) == 42:       # Randomly return nothing (which is an error)
                retval = None
                ec.report_exit_call_error(error_name="DBError", error_message="No records returned")

            if randint(1, 100) >= 95:       # Also randomly make things take longer
                time.sleep(uniform(0, 1))

    return retval
