/**
 * Fixture generator for the Paladin Java SDK POC differential test.
 *
 * Spins up a tiny HTTP server, points the official TypeScript SDK at it,
 * invokes ptx.sendTransaction with a known input, captures the exact
 * JSON-RPC request body the SDK sent, and writes it to fixtures/.
 *
 * The captured fixture is the reference truth. The Java SDK's contract
 * test asserts that, given the equivalent input, it produces the same
 * wire-level request body.
 *
 * Run: npm run generate
 */

const express = require("express");
const fs = require("fs");
const path = require("path");
const PaladinClient = require("@lfdecentralizedtrust/paladin-sdk").default;

const PORT = 18081;
const FIXTURES_DIR = path.join(__dirname, "fixtures");

// The transaction we're capturing. This must stay in sync with the
// TransactionInput the Java contract test constructs in Gate 6.
const TRANSACTION_INPUT = {
  idempotencyKey: "idem-key-001",
  type: "private",
  domain: "noto",
  from: "alice@node1",
  to: "0xabcdef",
  function: "transfer",
  data: "0x010203",
};

const FIXTURE_FILENAME = "ptx_sendTransaction.json";

async function captureFixture() {
  // Make sure the fixtures directory exists.
  fs.mkdirSync(FIXTURES_DIR, { recursive: true });

  // Set up an Express server that captures the first POST it receives,
  // responds with a valid JSON-RPC success envelope, and shuts itself down.
  const app = express();
  app.use(express.json());

  let capturedBody = null;

  const serverPromise = new Promise((resolve, reject) => {
    app.post("/", (req, res) => {
      capturedBody = req.body;
      // Respond with a syntactically-valid JSON-RPC result so the SDK
      // returns cleanly. The result string is arbitrary; the SDK doesn't
      // care what we return for this fixture run.
      res.json({
        jsonrpc: "2.0",
        id: req.body.id,
        result: "0x0000000000000000000000000000000000000000000000000000000000000001",
      });
    });

    const server = app.listen(PORT, () => {
      console.log(`Mock server listening on port ${PORT}`);
      resolve(server);
    });
    server.on("error", reject);
  });

  const server = await serverPromise;

  try {
    // Invoke the real TS SDK against our mock server.
    const client = new PaladinClient({
      url: `http://localhost:${PORT}`,
    });

    console.log("Calling client.ptx.sendTransaction(...)...");
    const txId = await client.ptx.sendTransaction(TRANSACTION_INPUT);
    console.log(`SDK returned txId: ${txId}`);

    if (!capturedBody) {
      throw new Error("No request body was captured. SDK call did not hit our server.");
    }

    // Strip the dynamically-allocated request id before persisting.
    // The id is per-call transport machinery (the TS SDK uses Date.now()),
    // not part of the SDK's wire-format contract. Both SDKs must produce
    // matching method names and params; the id is asserted separately
    // as "any positive integer" in the Java contract test.
    const stableBody = {
      jsonrpc: capturedBody.jsonrpc,
      method: capturedBody.method,
      params: capturedBody.params,
    };

    const fixturePath = path.join(FIXTURES_DIR, FIXTURE_FILENAME);
    fs.writeFileSync(
      fixturePath,
      JSON.stringify(stableBody, null, 2) + "\n"
    );

    console.log(`Fixture written to: ${fixturePath}`);
    console.log(`(Note: 'id' field stripped from persisted fixture; it is dynamic per-call.)`);
    console.log("Captured body (full, including id, for verification):");
    console.log(JSON.stringify(capturedBody, null, 2));
  } finally {
    server.close();
  }
}

captureFixture().catch((err) => {
  console.error("Fixture generation failed:");
  console.error(err);
  process.exit(1);
});
