# DCSA Verified Gross Mass (VGM) API

This is the OpenAPI specification of the **DCSA Verified Gross Mass (VGM)** standard.

This API allows the transfer of structured VGM data from a VGM Producer to a VGM Consumer.

Each VGM Producer implements the `GET /vgms` endpoint, which can be called by the authorized VGM Consumers to retrieve relevant available VGMs.

Each VGM Consumer implements the `POST /vgms` endpoint, which can be called by VGM Producers call to send relevant VGMs as they become available.

The registration of VGM Consumers with VGM Producers is out of scope.

The authentication and authorization in both directions between VGM Producers and VGM Consumers is out of scope.

### Work in progress ⚠️

The DCSA Verified Gross Mass (VGM) standard, including this API, is currently being designed and is **not** ready for general adoption yet.
