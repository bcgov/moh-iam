function getParameterByName(name, url) {
    if (!url) url = window.location.href;
    name = name.replace(/[\[\]]/g, '\\$&');
    var regex = new RegExp('[?&]' + name + '(=([^&#]*)|&|#|$)'),
            results = regex.exec(url);
    if (!results) return null;
    if (!results[2]) return '';
    return decodeURIComponent(results[2].replace(/\+/g, ' '));
};

function hideIdProviders(idProviders) {
    if (getParameterByName('idps_to_show') != null && getParameterByName('idps_to_show').toLowerCase().indexOf('all') === -1) {
        for (var i = 0; i < idProviders.length; i++) {
            if (getParameterByName('idps_to_show').toLowerCase().split(',').indexOf(idProviders[i].toLowerCase()) === -1) {
                document.getElementById('zocial-' + idProviders[i]).style.display = 'none';
            }
        }
    }
};

// When the user clicks on the button, open the modal
function showInstructionsModal() {
    var modal = document.getElementById("instructions-modal");
    modal.style.display = "block";
};

function hideInstructionsModal() {
    var modal = document.getElementById("instructions-modal");
    modal.style.display = "none";
};

// When the user clicks anywhere outside of the modal, close it
window.onclick = function(event) {
    var modal = document.getElementById("instructions-modal");
    if (event.target == modal) {
        modal.style.display = "none";
    }
};
