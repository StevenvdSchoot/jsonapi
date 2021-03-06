#!/usr/bin/env php
<?php

$host = "localhost";
$port = 20059;
$endpoint = "api/2/call";

$username = "usernameGoesHere";
$password = "passwordGoesHere";
$username = "test";
$password = "test";
$salt = "salt goes here";

function gen_key($name) {
	global $username, $password, $salt;
	return hash('sha256', $username . $name . $password . $salt);
}


$methodName = "getPlayers";

$payload = array(
	array(
		'name' => $methodName,
		'key' => gen_key($methodName),
		'username' => $username,
	),
	array(
		'name' => 'getWorlds',
		'key' => gen_key('getWorlds'),
		'username' => $username,
	),
	array(
		'name' => 'system.getServerClockDebug',
		'key' => gen_key('system.getServerClockDebug'),
		'username' => $username,
	),
	array(
		'name' => 'system.getJavaMemoryUsage',
		'key' => gen_key('system.getJavaMemoryUsage'),
		'username' => $username,
	),
	array(
		'name' => 'system.getJavaMemoryTotal',
		'key' => gen_key('system.getJavaMemoryTotal'),
		'username' => $username,
	),
	array(
		'name' => 'system.getDiskUsage',
		'key' => gen_key('system.getDiskUsage'),
		'username' => $username,
	),
	array(
		'name' => 'system.getDiskSize',
		'key' => gen_key('system.getDiskSize'),
		'username' => $username,
	)
);

$streamPayload = array(
	array(
		'name' => 'console',
		'key' => gen_key('console'),
		'username' => $username,
		'tag' => 'console',
		'show_previous' => true,
	)
);

if(!(php_sapi_name() == 'cli' && empty($_SERVER['REMOTE_ADDR']))) {
	echo "<pre>";
}

$stream = true;
if($stream) {
	$url = sprintf("http://%s:%d/api/2/subscribe?json=%s", $host, $port, rawurlencode(json_encode($streamPayload)));
	echo $url ."\n";
	exit();	
}

$url = sprintf("http://%s:%d/%s?json=%s", $host, $port, $endpoint, rawurlencode(json_encode($payload)));
echo $url ."\n\n\n";

$c = curl_init($url);
curl_setopt($c, CURLOPT_PORT, $port);
curl_setopt($c, CURLOPT_HEADER, true);
curl_setopt($c, CURLOPT_RETURNTRANSFER, true);
curl_setopt($c, CURLOPT_TIMEOUT, 10);		

curl_setopt($c, CURLOPT_VERBOSE, 0);

$response = curl_exec($c);

// Then, after your curl_exec call:
$header_size = curl_getinfo($c, CURLINFO_HEADER_SIZE);
$header = substr($response, 0, $header_size);
$body = json_decode(substr($response, $header_size), true);

curl_close($c);

echo $header;

print_r($body);
