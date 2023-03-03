# ServiceNow Batch Sink

Description
-----------

Writes to the specified table within ServiceNow. All the fields in the source table must match with the fields in the 
destination table. For update operations, sys_id must be present.

Properties
----------

**Reference Name**: Name used to uniquely identify this source for lineage, annotating metadata, etc.

**Table Name**: The name of the ServiceNow table into which data is to be pushed.

**Client ID**: The Client ID for ServiceNow Instance.

**Client Secret**: The Client Secret for ServiceNow Instance.

**REST API Endpoint**: The REST API Endpoint for ServiceNow Instance. For example, `https://instance.service-now.com`

**User Name**: The user name for ServiceNow Instance.

**Password**: The password for ServiceNow Instance.

**Use Connection:** Whether to use a connection. If a connection is used, you do not need to provide the credentials.

**Connection:** Name of the connection to use. Table Names information will be provided by the connection.
You also can use the macro function ${conn(connection-name)}.


**Operation** The type of operation to be performed. Insert operation will insert the data. Update operation will update
existing data in the table. "sys_id" must be present in the records.

Data Types Mapping
----------

    | CDAP Schema Data Type          | ServiceNow Data Type  | Comment                                            |
    | ------------------------------ | --------------------- | -------------------------------------------------- |
    | Boolean                        | boolean               |                                                    |
    | int/ long                      | integer(max length 40 |                                                    |
    | Decimal                        | Decimal(precision 20 with  18 before decimal point and scale is 2)         |     
    | array                          | unsupported           |                                                    |
    | bytes                          | unsupported           |                                                    |
    | Date                           | glide_date(yyyy-MM-dd)|                                                    |
    | datetime                       | glide_date_time(yyyy-MM-dd hh:mm:ss)                                       |                                                   
    | bigdecimal                     | Decimal               |                                                    |
    | double / float                 | Decimal               |                                                    |
    | map                            | Unsupported           |                                                    |
    | record                         | Unsupported           |                                                    |
    | time                           | glide_time            |                                                    |
    | timestamp                      | glide_date_time       |                                                    |
