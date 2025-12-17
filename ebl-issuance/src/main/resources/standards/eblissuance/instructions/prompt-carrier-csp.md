## Certificate Submission Instructions

To enable our platform to validate the digital signatures on your API requests, please provide your X.509 certificate
using the format described below. Your private key must remain with you and must never be shared.

### **Generate Your Key Pair and Certificate**

You must generate your own key pair:

**Private key**

- Keep securely.
- Use it to sign the payload of each API request.

**X.509 certificate containing your public key**

- Create an X.509 certificate using your public key.
- This certificate must be shared with our platform.
- We will extract the public key from the certificate to verify your request signatures.

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