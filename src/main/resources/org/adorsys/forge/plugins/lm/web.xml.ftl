<?xml version="1.0" encoding="UTF-8"?>

<!-- Good old fashion web.xml is used to configure the webapp. When the web 
	server (e.g. Tomcat or Jetty) starts up, it will read this file to determine 
	which URLs to map to where, etc. In our case most of this work is going to 
	be taken care of by SpringMVC so we simply have to tell the web server to 
	delegate everything through to it. -->

<web-app xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xmlns="http://java.sun.com/xml/ns/javaee"
	xsi:schemaLocation="http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd"
	version="2.5">

	<security-constraint>
		<web-resource-collection>
			<web-resource-name>All Access</web-resource-name>
			<url-pattern>/rest/*</url-pattern>
			<http-method>DELETE</http-method>
			<http-method>PUT</http-method>
			<http-method>HEAD</http-method>
			<http-method>OPTIONS</http-method>
			<http-method>TRACE</http-method>
			<http-method>GET</http-method>
			<http-method>POST</http-method>
		</web-resource-collection>
		<auth-constraint >
			<role-name>user</role-name>
		</auth-constraint>

		<user-data-constraint>
			<transport-guarantee>NONE</transport-guarantee>
		</user-data-constraint>
	</security-constraint>
	<login-config>
		<auth-method>FORM</auth-method>
		<form-login-config>
			<form-login-page>/login</form-login-page>
			<form-error-page>/login-failed</form-error-page>
		</form-login-config>
	</login-config>
</web-app>