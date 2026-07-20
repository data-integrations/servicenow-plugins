# ServiceNow Connection

Description
-----------
Use this connection to access data in ServiceNow.

Properties
----------
**Name:** Name of the connection. Connection names must be unique in a namespace.

**Description:** Description of the connection.

**Client ID**: The Client ID for ServiceNow Instance.

**Client Secret**: The Client Secret for ServiceNow Instance.

**REST API Endpoint**: The REST API Endpoint for ServiceNow Instance. 

**User Name**: The user name for ServiceNow Instance.

**Password**: The password for ServiceNow Instance.

**Proxy URL**: Proxy URL through which all the ServiceNow API calls are routed. For example,
`http://proxy.example.com:8080`. If no scheme is specified (for example, `proxy.example.com:8080`),
it defaults to `http`. Specify `https://` explicitly
when using an HTTPS proxy. Leave it empty to connect to ServiceNow directly.

**Proxy Username**: The username to authenticate with the proxy server, if the proxy requires authentication.

**Proxy Password**: The password to authenticate with the proxy server, if the proxy requires authentication.


Path of the connection
----------------------
To browse, get a sample from, or get the specification for this connection (Not supported in ServiceNow Batch Multi 
Source plugins).  
/{table} This path indicates a ServiceNow table. A table is the only one that can be sampled.