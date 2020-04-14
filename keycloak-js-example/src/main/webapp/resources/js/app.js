var app = (function () {
    var loadUserDetails = function () {
        var url = 'http://localhost:8081/auth/admin/realms/moh-users-realm/users?briefRepresentation=true&first=0&max=20&search=' + document.getElementById('user-name').value;

        var req = new XMLHttpRequest();
        req.open('GET', url, true);
        req.setRequestHeader('Accept', 'application/json');
        req.setRequestHeader('Authorization', 'Bearer ' + keycloak.token);

        req.onreadystatechange = function () {
            if (req.readyState === 4) {
                if (req.status === 200) {
                    output(this.responseText);
                } else if (req.status === 403) {
                    output('Forbidden');
                } else {
                    output('Failed');
                }
            }
        };

        req.send();
    };

    function loadData() {
        keycloak.updateToken().success(loadUserDetails);
    }


    function loadProfile() {
        keycloak.loadUserProfile().success(function (profile) {
            output(profile);
        }).error(function () {
            output('Failed to load profile');
        });
    }

    function updateProfile() {
        var url = keycloak.createAccountUrl().split('?')[0];
        var req = new XMLHttpRequest();
        req.open('POST', url, true);
        req.setRequestHeader('Accept', 'application/json');
        req.setRequestHeader('Content-Type', 'application/json');
        req.setRequestHeader('Authorization', 'bearer ' + keycloak.token);

        req.onreadystatechange = function () {
            if (req.readyState === 4) {
                if (req.status === 200) {
                    output('Success');
                } else {
                    output('Failed');
                }
            }
        };

        req.send('{"email":"myemail@foo.bar","firstName":"test","lastName":"bar"}');
    }

    function loadUserInfo() {
        keycloak.loadUserInfo().success(function (userInfo) {
            output(userInfo);
        }).error(function () {
            output('Failed to load user info');
        });
    }

    function refreshToken(minValidity) {
        keycloak.updateToken(minValidity).success(function (refreshed) {
            if (refreshed) {
                output(keycloak.tokenParsed);
            } else {
                output('Token not refreshed, valid for ' + Math.round(keycloak.tokenParsed.exp + keycloak.timeSkew - new Date().getTime() / 1000) + ' seconds');
            }
        }).error(function () {
            output('Failed to refresh token');
        });
    }

    function showExpires() {
        if (!keycloak.tokenParsed) {
            output("Not authenticated");
            return;
        }

        var o = 'Token Expires:\t\t' + new Date((keycloak.tokenParsed.exp + keycloak.timeSkew) * 1000).toLocaleString() + '\n';
        o += 'Token Expires in:\t' + Math.round(keycloak.tokenParsed.exp + keycloak.timeSkew - new Date().getTime() / 1000) + ' seconds\n';

        if (keycloak.refreshTokenParsed) {
            o += 'Refresh Token Expires:\t' + new Date((keycloak.refreshTokenParsed.exp + keycloak.timeSkew) * 1000).toLocaleString() + '\n';
            o += 'Refresh Expires in:\t' + Math.round(keycloak.refreshTokenParsed.exp + keycloak.timeSkew - new Date().getTime() / 1000) + ' seconds';
        }

        output(o);
    }

    function output(data) {
        if (typeof data === 'object') {
            data = JSON.stringify(data, null, '  ');
        }
        document.getElementById('output').innerHTML = data;
    }

    function event(event) {
        var e = document.getElementById('events').innerHTML;
        document.getElementById('events').innerHTML = new Date().toLocaleString() + "\t" + event + "\n" + e;
    }

    var keycloak = Keycloak();

    keycloak.onAuthSuccess = function () {
        event('Auth Success');
        document.getElementsByTagName("body")[0].style.cssText += 'display: block';
    };

    keycloak.onAuthError = function (errorData) {
        event("Auth Error: " + JSON.stringify(errorData));
    };

    keycloak.onAuthRefreshSuccess = function () {
        event('Auth Refresh Success');
    };

    keycloak.onAuthRefreshError = function () {
        event('Auth Refresh Error');
    };

    keycloak.onAuthLogout = function () {
        event('Auth Logout');
    };

    keycloak.onTokenExpired = function () {
        event('Access token expired.');
    };

    // Flow can be changed to 'implicit' or 'hybrid', but then client must enable implicit flow in admin console too 
    var initOptions = {
        responseMode: 'fragment',
        flow: 'standard',
        onLoad: 'login-required'
    };

    keycloak.init(initOptions).success(function (authenticated) {
        output('Init Success (' + (authenticated ? 'Authenticated' : 'Not Authenticated') + ')');
    }).error(function () {
        output('Init Error');
    });

    return {
        loadData: loadData,
        loadProfile: loadProfile,
        updateProfile: updateProfile,
        loadUserInfo: loadUserInfo,
        refreshToken: refreshToken,
        showExpires: showExpires,
        output: output,
        keycloak: keycloak
    };

})();
