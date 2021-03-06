/**
 * Shows a modal dialog and loads the content from url on it.
 * @param {String} url The URL to get the dialog content from.
 */
function showModal(url) {
    submitURL(url, "get", function(data) {
        $("#appModal").html(data);
    }, function () {
        var emsg = "Sorry, there was a problem processing the request. Try again later.";
        $("#appModal").find("#modal-content").html(" "+emsg);
    });
    
    /*var emsg = "Sorry, there was an error processing the request...";
    var jqxhr = $.get(url, function(data) {
        $("#appModal").html(data);
    })
    .fail(function() {
        $("#appModal").find("#modal-content").html(" "+emsg);
    });*/
}

/**
 * Enables forms validation using jqueri-validate plugin.
 */
function enableFormsValidation() {
    if ($('form').length) {
        $('form').validate({
            onBlur: true,
            onSubmit: true,
            invalid: function() {
                alert('Algunos de los datos no son correctos');
            },
            eachInvalidField : function() {
                $(this).closest('.form-group').addClass('has-error');
            },
            eachValidField: function() {
                $(this).closest('.form-group').removeClass('has-error');
            },
            description : {
                common: {
                    required : function() {
                        return $(this).data('msgrequired') || 'Este dato es requerido';
                    },
                    pattern : function() {
                        return $(this).data('msgerror') || 'El dato proporcionado no es válido';
                    }
                }
            }
        });
    }
}

/**
 * Makes an ajax request to a given url.
 * @param {String} url URL for the request.
 * @param {String} method GET or POST.
 * @param {function} success Handler for the success event.
 * @param {function} fail Handler for the fail event.
 */
function submitURL(url, method, success, fail) {
    var _method = method || "get";
    if (_method === "get") {
        var jqxhr = $.get(url, function(data) {
            if (success && typeof success === "function") {
                success(data);
            } else {
                console.log("OK");
            }
        })
        .fail(function() {
            if (fail && typeof fail === "function") {
                fail();
            } else {
                alert("There was a problem processing the request. Try again later.");
            }
        });
    } else if (_method === "post") {
        var jqxhr = $.post(url, function(data) {
            if (success && typeof success === "function") {
                success(data);
            }
        })
        .fail(function() {
            if (fail && typeof fail === "function") {
                fail();
            }
        });
    }
}

function knotsToKmH(knots) {
    var kt = ""+knots;
    return (parseFloat(kt)*1.852).toFixed();
}

//Add change event at level document for the file selector
$(document).on('change', '.btn-file :file', function() {
    var input = $(this),
    numFiles = input.get(0).files ? input.get(0).files.length : 1,
    label = input.val().replace(/\\/g, '/').replace(/.*\//, '');
    input.trigger('fileselect', [numFiles, label]);
});

//Init app
$(document).ready(function () {
    //Enable bootstrap tooltips
    if ($("[data-tooltip=tooltip]").length) {
        $("[data-tooltip=tooltip]").tooltip();
    }
    
    //Destroy bootstrap modal content each time the modal closes, reset map data
    $('#appModal').on('hidden.bs.modal', function () {
        $(this).removeData('bs.modal');
    });
    
    $('.btn-file :file').on('fileselect', function(event, numFiles, label) {
        var input = $(this).parents('.input-group').find(':text');
        if (input.length) {
            input.val(label);
        }
    });
    
    //Enable datetimepickers
    if ($('.datePicker').length) {
        $('.datePicker').datetimepicker({
            pickTime:false,
            useCurrent:true,
            useSeconds:false
        });
    }
    
    if ($('.timePicker').length) {
        $('.timePicker').datetimepicker({
            pickDate:false,
            useCurrent:true,
            useSeconds:false
        });
    }
    
    //Enable form validation
    enableFormsValidation();
});