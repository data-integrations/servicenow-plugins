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

**Operation** The type of operation to be performed. Insert operation will insert the data. Update operation will update
existing data in the table. "sys_id" must be present in the records.

**Max Records Per Batch** No. of requests that will be sent to ServiceNow Batch API as a payload. Rest API property in Transaction 
quota section "REST Batch API request timeout" should be increased to use higher records in a batch. By default this 
property has a value of 30 sec which can handle approximately 200 records in a batch. To use a bigger batch size, set it 
to a higher value.

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
