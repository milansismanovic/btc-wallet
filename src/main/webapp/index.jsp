<html>
<body>
	<h4>Bitcoin</h4>
	<h5>getTransactions</h5>
	<form action="webapi/bitcoin/getTransactions" method="get">
		<label>Get All User's Transactions </label><input type="submit" value="execute">
	</form>
	<h5>getBalance</h5>
	<form action="webapi/bitcoin/getBalance" method="get">
		<label>Get User's Balance </label><input type="submit" value="execute">
	</form>
	<h5>createAddress</h5>
	<form action="webapi/bitcoin/createAddress" method="get">
		<label>User's Public Key</label> <input type="text" name="publicKey" value="126zXovk8TzK6hq8m6jSEGHbRpuMhd6pnK">
		<label>create multi-signature address </label><input type="submit" value="execute"><br/>
		<label>associated private key: KxKGLjQDWSmYsx4wVxCSmPYN3DAGqb16ajy2wENgKtud9H4HvwJq</label><br/> 
		<label>associated public key: 047FCBD6F33D657D273BEE4D99C2BE78BE406FCBA2C63C0068CC77AFF2405C1AC50AB3023290EE81027A17B6E59A6A683CC4BE767A9118A404DDC7A83FEC8E731A</label>
	</form>
	<h5>startTransaction</h5>
	<form action="webapi/bitcoin/startTransaction" method="get">
		<label>Receiver Address</label> <input type="text" name="publicKey" value="1H6eogdwWeeNmeDtEJbSaogWDtjue3Rn9K">
		<label>Satoshi</label> <input type="text" name="publicKey" value="10000">
		<label>User's Change Address</label> <input type="text" name="publicKey" value="1H6eogdwWeeNmeDtEJbSaogWDtjue3Rn9K">
		<label>startTransaction </label><input type="submit" value="execute"><br/>
	</form>
	<h5>executeTransaction</h5>
	<form action="webapi/bitcoin/executeTransaction" method="get">
		<label>Signed Transaction</label> <input type="text" name="publicKey" value="signedTransactionDummy">
		<label>startTransaction </label><input type="submit" value="execute"><br/>
		<label>associated private key: KxKGLjQDWSmYsx4wVxCSmPYN3DAGqb16ajy2wENgKtud9H4HvwJq</label> 
	</form>

    <h2>Javadoc</h2>
    <p><a href="javadoc">Javadoc</a>

    <h2>Jersey RESTful Web Application!</h2>
    <p><a href="webapi/myresource">Jersey resource</a>
    <p>Visit <a href="http://jersey.java.net">Project Jersey website</a>
    for more information on Jersey!
</body>
</html>
