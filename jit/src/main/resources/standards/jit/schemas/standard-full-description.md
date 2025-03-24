# DCSA OpenAPI specification for Just in Time Port Call process

The DCSA API for **Just In Time Port Call** aims to simplify and standardize the exchange of operational information
between carriers, terminals, port authorities, and service providers, to orchestrate and optimize port calls. JIT can be
used for negotiating services execution via timestamps, informing timestamps, or informing moves forecasts, between the
parties involved in any given location.

The planning and execution of events always follow the same pattern, in which several instances of the estimated (`E`),
requested (`R`), and planned (`P`) times (from now on referred to as: `ERP`-pattern) can occur if a new estimated or
requested time is given after the initially planned time. Some events, specified in this document, do not need the `ERP`
-pattern, as they are informative and reflect actuals (`A`) only (i.e. vessel readiness for cargo operations).

For more information about the `ERP`-pattern please
check [GUIDELINES FOR HARMONIZED COMMUNICATION AND ELECTRONIC EXCHANGE OF OPERATIONAL DATA FOR PORT CALLS](https://wwwcdn.imo.org/localresources/en/OurWork/Facilitation/FAL%20related%20nonmandatory%20documents/FAL.5-Circ.52.pdf)
by IMO. Link to IMO
GIA [Just In Time Arrival Guide](https://greenvoyage2050.imo.org/wp-content/uploads/2021/01/GIA-just-in-time-hires.pdf).

## Port Call Services in the scope of this API

Negotiable **Port Call Services** through an `ERP`-pattern including an `A`:

- Berth
- Cargo operations
- Pilotage
- Towage
- Mooring
- Bunkering
- Pilot Boarding Place
- Anchorage
- Sludge

**Non**-negotiable **Port Call Services** (without `ERP`-pattern) having only an `A`:

- Sea Passage
- All Fast
- Gangway down and secure
- Vessel Ready for cargo operations
- Vessel Ready to sail
- Discharge cargo operations
- Loading cargo operations
- Lashing
- Safety - Terminal ready for vessel departure
- Anchorage Operations
- ShorePower

Also in scope:

- Moves forecast
- cancel (by **Service Provider**) or decline (by **Service Consumer**) of a **Port Call Service**
- omission of **Port Call** or **Terminal Call**

## How to create a Port Call Service

To request a **Port Call Service** do the following:

- Create a **Port Call** by calling the

       PUT /port-calls/{portCallID}

- Create a **Terminal Call** and link it to the **Port Call** created above by calling

       PUT /terminal-calls/{terminalCallID}

- Create a **Port Call Service** and link it to the **Terminal Call** created above by calling

       PUT /port-call-services/{portCallServiceID}

It is the responsibility of the **Service Provider** of the initial **Port Call Service** to create a:

- `portCallID` to identify all communication regarding the **Port Call**
- `terminalCallID` to identify each **Terminal Call** inside the same `portCallID`. One **Port Call** can contain many *
  *Terminal Calls**
- `portCallServiceID` to identify each **Port Call Service** inside the same `terminalCallID`. One **Terminal Call** can
  contain many **Port Call Services**

It is the responsibility of the creator of a **Timestamp** (Estimated, Requested, Planned or Actual) to create the
`timestampID` as an identifier for the **Timestamp** for any further referral. One **Port Call Service** can contain
many **Timestamps**.

### API Design & Implementation Principles

This API follows the guidelines defined in version 2.0 of the API Design & Implementation Principles which can be found
on the [DCSA Developer Portal](https://developer.dcsa.org/api_design).

For a changelog, please click [here](https://github.com/dcsaorg/DCSA-OpenAPI/tree/master/jit/v2#v200). If you have any
questions, feel free to [Contact Us](https://dcsa.org/get-involved/contact-us).
