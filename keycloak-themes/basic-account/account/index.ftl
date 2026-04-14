<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Personal Info</title>
    <style>
        :root {
            --bc-blue: rgb(0, 51, 102);
            --border: #d9d9d9;
            --label-bg: #f7f7f7;
            --text: #1a1a1a;
            --muted: #5c6670;
        }

        * {
            box-sizing: border-box;
        }

        body {
            margin: 0;
            font-family: Arial, sans-serif;
            color: var(--text);
            background: #fff;
        }

        .topbar {
            height: 72px;
            background: var(--bc-blue);
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 0 24px;
        }

        .topbar img {
            display: block;
            height: 40px;
        }

        .signout-btn {
            padding: 10px 16px;
            border: 1px solid #fff;
            background: transparent;
            color: #fff;
            font: inherit;
            font-weight: 700;
            cursor: pointer;
        }

        .signout-btn:hover {
            background: rgba(255, 255, 255, 0.12);
        }

        .layout {
            display: flex;
            min-height: calc(100vh - 72px);
        }

        .rail {
            flex: 0 0 220px;
            background: var(--bc-blue);
        }

        .content {
            flex: 1;
            max-width: 1100px;
            padding: 36px 32px 48px;
        }

        h1 {
            margin: 0 0 10px;
            font-size: 2rem;
        }

        .subtitle {
            margin: 0 0 32px;
            color: var(--muted);
        }

        table {
            width: 100%;
            max-width: 900px;
            border-collapse: collapse;
            background: #fff;
        }

        td {
            border: 1px solid var(--border);
            padding: 18px 20px;
            vertical-align: middle;
        }

        td.label {
            width: 260px;
            font-weight: 700;
            color: var(--bc-blue);
            background: var(--label-bg);
        }

        .error {
            margin-top: 16px;
            color: #b00020;
            font-weight: 700;
        }

        @media (max-width: 900px) {
            .layout {
                flex-direction: column;
            }

            .rail {
                flex-basis: 40px;
            }

            .content {
                padding: 24px 16px 32px;
            }

            td.label {
                width: 180px;
            }
        }
    </style>
</head>
<body>
<header class="topbar">
    <img
            src="https://www.health.gov.bc.ca/assets/logo.svg"
            alt="Government of British Columbia"
    />
    <button type="button" id="signOut" class="signout-btn">Sign out</button>
</header>

<div class="layout">
    <aside class="rail" aria-hidden="true"></aside>

    <main class="content">
        <h1>Personal info</h1>
        <p class="subtitle">Basic information from your sign-in token.</p>

        <table>
            <tr>
                <td class="label">Username</td>
                <td id="username">Loading...</td>
            </tr>
            <tr>
                <td class="label">Email</td>
                <td id="email">Loading...</td>
            </tr>
            <tr>
                <td class="label">First name</td>
                <td id="firstName">Loading...</td>
            </tr>
            <tr>
                <td class="label">Last name</td>
                <td id="lastName">Loading...</td>
            </tr>
        </table>

        <div id="error" class="error" hidden></div>
    </main>
</div>

<script type="module">
    // Pinned Keycloak JS version to match server compatibility.
    import Keycloak from '/assets/keycloak-js-26.2.3.js';

    const pathParts = window.location.pathname.split('/');
    const realmIndex = pathParts.indexOf('realms');
    const realm = realmIndex >= 0 ? pathParts[realmIndex + 1] : 'moh_applications';

    const keycloak = new Keycloak({
        url: window.location.origin + '/auth/',
        realm: realm,
        clientId: 'account-console'
    });

    const setText = (id, value) => {
        document.getElementById(id).textContent = value || '';
    };

    const showError = (message) => {
        const error = document.getElementById('error');
        error.textContent = message;
        error.hidden = false;
    };

    document.getElementById('signOut').addEventListener('click', () => {
        keycloak.logout();
    });

    keycloak.init({
        onLoad: 'login-required',
        checkLoginIframe: false
    })
        .then((authenticated) => {
            if (!authenticated) {
                showError('Authentication failed.');
                return;
            }

            const token = keycloak.idTokenParsed || {};
            setText('username', token.preferred_username);
            setText('email', token.email);
            setText('firstName', token.given_name);
            setText('lastName', token.family_name);
        })
        .catch((error) => {
            console.error('Keycloak init failed', error);
            showError('Could not load user information.');
        });
</script>
</body>
</html>
