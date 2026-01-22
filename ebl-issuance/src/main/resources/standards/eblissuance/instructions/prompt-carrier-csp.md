## Certificate Submission Instructions

To enable our platform to validate the digital signatures on your API requests, please provide your X.509 certificate
using the format described below. Your private key must remain with you and must never be shared.

### **Generate Your Key Pair and Certificate**

**Step 1: Generate a cryptographic key pair (private + public key)**

Choose one of the supported key types and generate your key pair:

- **RSA**: 2048-bit or 4096-bit key size
- **ECDSA**: P-256, P-384, or P-521 elliptic curves

The private key must be kept securely and never shared. You will use it to sign API request payloads.

**Step 2: Create an X.509 certificate containing your public key**

- Generate an X.509 certificate from the key pair created in Step 1.
- This certificate contains your public key and must be shared with our platform.
- We will extract the public key from the certificate to verify your request signatures.
- **Important**: Only X.509 certificates with RSA or ECDSA public keys are supported.

**Step 3: Sign your API requests** *(for future use after certificate submission)*

When making API requests to our platform, you will need to sign the payload using your private key with one of these JWS algorithms:
- For RSA keys: RS256, RS384, RS512, PS256, PS384, or PS512
- For ECDSA keys: ES256, ES384, or ES512

The signing algorithm must be compatible with your key type from Step 1.

### **Prepare Your X.509 Certificate**

Export your certificate in PEM (.pem) format and convert it into the required single-line format.

Follow these steps:

1. Export your certificate in standard PEM format.

2. Remove all newline characters from the Base64 content so that it becomes a single continuous line.

3. Keep the header and footer exactly unchanged.

4. Insert the literal characters `\r\n`:
    - immediately after the header
    - and immediately before the footer

5. The final result must be provided as one single line.