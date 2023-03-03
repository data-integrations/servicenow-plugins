# ServiceNow Batch Source

Description
-----------

Reads from one or multiple tables within ServiceNow depending on the mode value set for this plugin. In case of 
`Reporting` mode, the source will output a record for each row in the table it reads, with each record containing 
an additional field that holds the name of the table the record came from. In case of `Table` mode, this additional 
field will not be there in the output. In addition, for each table that will be read, this plugin will set pipeline 
arguments where the key is `multisink.[tablename]` and the value is the schema of the table. 

Properties
----------

**Reference Name**: Name used to uniquely identify this source for lineage, annotating metadata, etc.

**Mode**: Mode of query. The mode can be one of two values: 

`Reporting` - will allow user to choose application for which data will be fetched for all tables, 

`Table` - will allow user to enter table name for which data will be fetched.

**Application Name**: Application name for which data to be fetched. The application can be one of three values:  

`Contract Management` - will fetch data for all tables under [Contract Management](https://docs.servicenow.com/bundle/newyork-it-service-management/page/product/contract-management/reference/r_TablesInstalledWContractMgmt.html) 
application, 

`Product Catalog` - will fetch data for all tables under [Product Catalog](https://docs.servicenow.com/bundle/newyork-it-service-management/page/product/product-catalog/reference/r_TablesProductCatalog.html) 
application,

`Procurement` - will fetch data for all tables under [Procurement](https://docs.servicenow.com/bundle/newyork-it-service-management/page/product/procurement/reference/r_TablesProcurement.html) 
application.

Note, the Application name value will be ignored if the Mode is set to `Table`.

**Table Name Field**: The name of the field that holds the table name. Must not be the name of any table column that 
will be read. Defaults to `tablename`. Note, the Table name field value will be ignored if the Mode is set to `Table`.

**Table Name**: The name of the ServiceNow table from which data to be fetched. Note, the Table name value will be 
ignored if the Mode is set to `Reporting`.

**Client ID**: The Client ID for ServiceNow Instance.

**Client Secret**: The Client Secret for ServiceNow Instance.

**REST API Endpoint**: The REST API Endpoint for ServiceNow Instance. For example, `https://instance.service-now.com`

**User Name**: The user name for ServiceNow Instance.

**Password**: The password for ServiceNow Instance.

**Use Connection:** Whether to use a connection. If a connection is used, you do not need to provide the credentials.

**Connection:** Name of the connection to use. Table Names information will be provided by the connection.
You also can use the macro function ${conn(connection-name)}.


**Start Date**: The Start date to be used to filter the data. The format must be `yyyy-MM-dd`.

**End Date**: The End date to be used to filter the data. The format must be `yyyy-MM-dd`.

**Type of values**: The type of values to be returned. The type can be one of two values: 

`Actual` -  will fetch the actual values from the ServiceNow tables,  

`Display` - will fetch the display values from the ServiceNow tables.

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

