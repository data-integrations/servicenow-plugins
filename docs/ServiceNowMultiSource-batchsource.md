# ServiceNow Batch Multi Source

Description
-----------

Reads from one or multiple tables within ServiceNow

Properties
----------

**Reference Name**: Name used to uniquely identify this source for lineage, annotating metadata, etc.

**Table Names**: The name of the ServiceNow table(s) from which data to be fetched.

**Client ID**: The Client ID for ServiceNow Instance.

**Client Secret**: The Client Secret for ServiceNow Instance.

**REST API Endpoint**: The REST API Endpoint for ServiceNow Instance. For example, `https://instance.service-now.com`

**User Name**: The user name for ServiceNow Instance.

**Password**: The password for ServiceNow Instance.

**Start Date**: The Start date to be used to filter the data. The format must be `yyyy-MM-dd`.

**End Date**: The End date to be used to filter the data. The format must be `yyyy-MM-dd`.

**Type of values**: The type of values to be returned. The type can be one of two values: 

`Actual` -  will fetch the actual values from the ServiceNow tables,  

`Display` - will fetch the display values from the ServiceNow tables.

**Table Name Field**: The name of the field that holds the table name. Must not be the name of any table column that
will be read. Defaults to `tablename`. Note, the Table name field value will be ignored if the Mode is set to `Table`.

Data Types Mapping
----------

    | ServiceNow Data Type           | CDAP Schema Data Type | Comment                                            |
    | ------------------------------ | --------------------- | -------------------------------------------------- |
    | decimal                        | double                |                                                    |
    | integer                        | int                   |                                                    |
    | boolean                        | boolean               |                                                    |
    | reference                      | string                |                                                    |
    | currency                       | string                |                                                    |
    | glide_date                     | string                |                                                    |
    | glide_date_time                | string                |                                                    |
    | sys_class_name                 | string                |                                                    |
    | domain_id                      | string                |                                                    |
    | domain_path                    | string                |                                                    |
    | guid                           | string                |                                                    |
    | translated_html                | string                |                                                    |
    | journal                        | string                |                                                    |
    | string                         | string                |                                                    |
