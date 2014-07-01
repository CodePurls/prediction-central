function Client(version, transport) {
  this.version = version;
  this.transport = transport;
};

function Transport() {
  var transport = this;
  this.doXHR = function(method, url, obj, callbacks) {
    switch (method) {
    case "PUT":
    case "POST":
      data = JSON.stringify(obj);
      break;
    default:
      data = obj;
    }
    $.ajax({
      type : method,
      url : url,
      data : data,
      contentType : 'application/json',
      success : function(/* PlainObject */data, /* String */textStatus, /* jqXHR */jqXHR) {
        callbacks.onSuccess(data, textStatus, jqXHR);
      },
      error : function(/* jqXHR */jqXHR, /* String */textStatus, /* String */errorThrown) {
        callbacks.onError(jqXHR, textStatus, errorThrown);
      }
    });
  },

  this.doGet = function(url, obj, callbacks) {
    transport.doXHR('GET', url, obj, callbacks);
  };

  this.doPost = function(url, obj, callbacks) {
    transport.doXHR('POST', url, obj, callbacks);
  };

  this.doPut = function(url, obj, callbacks) {
    transport.doXHR('PUT', url, obj, callbacks);
  };

  this.doDelete = function(url, obj, callbacks) {
    transport.doXHR('DELETE', url, obj, callbacks);
  };

};

Client.prototype = {
  apiRoot : "/api/predictions",
  getPredictions : function(page, size, callbacks) {
    this.transport.doGet(this.apiRoot, {
      page : page,
      size : size
    }, callbacks);
  },

  getPrediction : function(id, callbacks) {
    this.transport.doGet(this.apiRoot + "/" + id, {}, callbacks);
  },

  createPrediction : function(obj, callbacks) {
    obj.ts = new Date().getTime();
    this.transport.doPost(this.apiRoot, obj, callbacks);
  },

  updatePrediction : function(id, obj, callbacks) {
    this.transport.doPut(this.apiRoot + "/" + id, obj, callbacks);
  },

  deletePrediction : function(id, callbacks) {
    this.transport.doDelete(this.apiRoot + "/" + id, {}, callbacks);
  },

  voteUp : function(id, callbacks) {
    this.transport.doPut(this.apiRoot + "/" + id + "/up", {}, callbacks);
  },

  voteDown : function(id, callbacks) {
    this.transport.doPut(this.apiRoot + "/" + id + "/down", {}, callbacks);
  },

  createComment : function(id, comment, callbacks) {
    this.transport.doPost(this.apiRoot + "/" + id + "/comments", comment, callbacks);
  },

  createUser : function(user, callbacks) {
    this.transport.doPost(this.apiRoot + "/users", user, callbacks);
  },

  isLoggedIn : function(callback) {
    var callbacks = {
      onSuccess : function(data, textStatus, jqXHR) {
        callback(data.email, data);
      },

      onError : function(jqXHR, textStatus, errorThrown) {
        console.log(textStatus + ", " + errorThrown);
        callback(false);
      }
    };
    this.transport.doGet(this.apiRoot + "/users/current", {}, callbacks);
  },

  login : function(obj, callbacks) {
    this.transport.doPost(this.apiRoot + "/users/login/" + new Date().getTime(), obj, callbacks);
  },
  getNextCaptcha : function(callbacks) {
    this.transport.doGet(this.apiRoot + "/captcha", {}, callbacks);
  },
  getCounts : function(callbacks) {
    this.transport.doGet(this.apiRoot + "/count", {}, callbacks);
  },

  search : function(q, callbacks) {
    this.transport.doGet(this.apiRoot + "/search", {
      q : q
    }, callbacks);
  },
  approve : function(id, callbacks) {
    this.transport.doPut(this.apiRoot + "/approve/" + id, {}, callbacks);
  },
  reject : function(id, callbacks) {
    this.transport.doPut(this.apiRoot + "/reject/" + id, {}, callbacks);
  },
  getAnalytics : function(days, top, callbacks) {
    days = days || 30;
    top = top || 10;
    this.transport.doGet(this.apiRoot + "/analytics", {
      days : days,
      top : top
    }, callbacks);
  }

};

function MPFront() {
  this.apiClient = new Client("v1", new Transport());
  this.PAGER_HEAD_TEMPLATE = '<li id="pagerp"><a href="javascript:void(0);" onclick="javascript:UI.loadPreviousPage(this)">&laquo; Previous</a></li>';
  this.PAGER_ITEM_TEMPLATE = '<li><a href="javascript:void(0);" onclick="javascript:UI.loadPredictionsPage($PAGENUM)">$PAGENUM</a></li>';
  this.PAGER_ACTIVE_ITEM_TEMPLATE = '<li class="active"><a href="javascript:void(0);" onclick="javascript:UI.loadPredictionsPage($PAGENUM)">$PAGENUM</a></li>';
  this.PAGER_TAIL_TEMPLATE = '<li id="pagern"><a href="javascript:void(0);" onclick="javascript:UI.loadNextPage(this)">Next &raquo;</a></li>';
  this.HAS_MORE_TEMPLATE = '<a class="btn " href="#p/$PID" >More.. &raquo;</a>';
  this.currentPage = 1;
  this.PAGE_SIZE = 10;
  this.remaining = this.PAGE_SIZE;
  this.isLoggedIn = false;
  this.currentUser;
  this.ERRORS = {
    "required" : "Value for this field is required!",
    "invalid" : "Value for this field is invalid."
  };
  this.BRAND_LOGOS = {
    "Twitter" : "https://abs.twimg.com/a/1381897480/images/resources/twitter-bird-light-bgs.png",
    "Facebook" : "https://fbcdn-dragon-a.akamaihd.net/hphotos-ak-prn1/851583_394483907322300_35050254_n.png",
    "GooglePlus" : "https://ssl.gstatic.com/images/icons/gplus-16.png",
    "Web" : "favicon.ico"
  };
}

MPFront.prototype = {
  navigate : function(hash) {
    if (!hash) {
      UI.loadPredictions();
      return;
    }
    var tokens = hash.substring(1).split("/");
    if (tokens.length !== 2) {
      if (tokens[0] === 'analytics') {
        UI.loadAnalytics();
        return;
      }
      console.log("Invalid location: " + hash);
      UI.loadPredictions();
      return;
    }
    var type = tokens[0];
    var id = tokens[1];
    if (type === 'p') {
      this.apiClient.getPrediction(id, {
        onSuccess : function(data, textStatus, jqXHR) {
          UI._renderSingle(data);
        },
        onError : function(jqXHR, textStatus, errorThrown) {
          console.log(jqXHR);
        }
      });
    } else if (type === 'page') {
      UI.loadPredictionsPage(id);
    } else {
      UI.loadPredictions();
    }
  },

  navigateToCurrentPage : function() {
    window.location.hash = "page/" + this.currentPage;
  },

  init : function(single) {
    $(window).on('hashchange', function() {
      UI.navigate(window.location.hash);
    });

    $('[data-toggle=offcanvas]').click(function() {
      $('.row-offcanvas').toggleClass('active');
    });
    $('#makePred').on('shown.bs.modal', function() {
      $('#prediction-text').focus();
    });
    $('#login').on('shown.bs.modal', function() {
      $('#user-login-email').focus();
    });
    $('#signup').on('shown.bs.modal', function() {
      $('#user-captcha-label').html('Loading captcha...');
      $('#user-captcha-label').attr('data-value', -1);
      var callbacks = {
        onSuccess : function(data, textStatus, jqXHR) {
          $('#user-captcha-label').html(data.captcha);
          $('#user-captcha-label').attr('data-value', data.captchaId);
          $('#user-name').focus();
        },
        onError : function(jqXHR, textStatus, errorThrown) {
          UI.showXHRError(jqXHR, textStatus, errorThrown);
        }
      };
      UI.apiClient.getNextCaptcha(callbacks);
    });
    try{

    $('#tsearch').typeahead({
      name : 'mpsearch',
      valueKey : "word",
      remote : '/api/predictions/search/suggest?term=%QUERY'
    });
    }catch(e){
        console.log(e);
    }
    $('.tt-query').css('background-color', '#fff');
    UI.apiClient.isLoggedIn(function(isLoggedIn, user) {
      UI.isLoggedIn = isLoggedIn;
      UI.currentUser = user;
      if (isLoggedIn) {
        var html = $('#signedas-area').html();
        html = html.replace(/\$USER/g, user.email);
        $('#signedas-area').html(html);
        $('#signedas-area').show();
      } else {
        $('#signin-area').show();
      }
      if (window.location.hash) {
        UI.navigate(window.location.hash);
      } else if (!single) {
        UI.loadPredictions();
      }
      UI.loadCounts();
    });
  },

  loadCounts : function() {
    var callbacks = {
      onSuccess : function(data, textStatus, jqXHR) {
        $('#ucount').text(data.users);
        $('#vcount').text(data.visitors);
        $('#pcount').text(data.predictions);
      },

      onError : function(jqXHR, textStatus, errorThrown) {
        $('#ucount').text(0);
        $('#vcount').text(0);
        $('#pcount').text(0);
      }
    };
    this.apiClient.getCounts(callbacks);
  },
  loadPreviousPage : function(e) {
    if ($('#pagerp').hasClass('disabled')) {
      return;
    }
    this.currentPage--;
    this.navigateToCurrentPage();
  },

  loadNextPage : function(e) {
    if ($('#pagern').hasClass('disabled')) {
      return;
    }
    this.currentPage++;
    this.navigateToCurrentPage();
  },

  loadPredictionsPage : function(page) {
    this.currentPage = page;
    this.loadPredictions();
  },

  _renderSingle : function(data) {
    $('#page-body').html('');
    var tmpl = $("#pred-single-item-container").html();
    var p = data;
    var html = UI.getPredictionHtmlExt(tmpl, p);
    $(html).attr('id', 'pred-single-item-container-' + p.id);
    $('#page-body').append(html);
    UI.handleVotingFor(p.id);
    window.scrollTo(0, 0);
  },

  getPredictionHtmlExt : function(tmpl, p) {
    var t = this.getPredictionHtml(tmpl, p);
    var commentTmpl = $("#comment-container").html();
    var commentsTmpl = $("#comments-container").html();
    var commentHtml = '';

    if (p.comments.length > 0) {

      for ( var i = 0; i < p.comments.length; i++) {
        var c = p.comments[i];
        commentHtml += commentTmpl.replace(/\$AUTHOR/g, c.author).replace(/\$COMMENT/g, c.comment);
      }

    }

    commentsTmpl = commentsTmpl.replace(/\$COMMENTAREA/g, commentHtml).replace(/\$CURRENTUSERNAME/g, UI.currentUser ? UI.currentUser.fullName : "").replace(/\$PID/g, p.id);

    if (!UI.currentUser || !UI.currentUser.registered) {
      commentsTmpl = commentsTmpl.replace(/\$CAPTCHA/g, 'Loading...');
      var callbacks = {
        onSuccess : function(data, textStatus, jqXHR) {
          var newTmpl = $("#comment-captcha").html();
          newTmpl = newTmpl.replace(/$CAPTCHAID/g, data.captchaId).replace(/$CAPTCHAEXPR/g, data.captcha);

        },
        onError : function(jqXHR, textStatus, errorThrown) {
          UI.showXHRError("Error loading captcha", jqXHR, textStatus, errorThrown);
        }
      };
      this.apiClient.getNextCaptcha(callbacks);
    } else {
      commentsTmpl = commentsTmpl.replace(/\$CAPTCHA/g, '').replace(/\$UDISABLED/g, "");
    }
    t = t.replace(/\$COMMENTSCONTAINER/g, commentsTmpl);
    return t;
  },

  getPredictionHtml : function(tmpl, p) {
    var t = tmpl.replace(/\$TITLE/g, p.title).replace(/\$PDATE/g, "Predicted for " + new Date(p.time).toUTCString() + ",").replace(/\$DATE/g, new Date(p.created_on).toLocaleString())
        .replace(/\$TAGS/g, p.tags).replace(/\$AUTHOR/g, p.original_author || p.createdByUser).replace(/\$PID/g, p.id).replace(/\$UPS/g, p.upvotes).replace(/\$DOWNS/g, p.downvotes)
        .replace(/\$COMMENTCOUNT/g, p.commentCount).replace(/\$SUBMITTER/g, p.original_author ? p.original_author : p.createdByUser.indexOf('Anonymous') !== -1 ? '' : p.createdByUser)
        .replace(/\$TEXT/g, p.text.replace('\n', '&nbsp;'));
    var brandId = p.createdByUser.indexOf("Crawler") === -1 ? 'Web' : p.createdByUser.replace("Crawler", "").trim();
    var href = UI.BRAND_LOGOS[brandId];
    if (!p.approved) {
      var modTmpl = $('#moderation-container').html();
      t = t.replace(/\$MODERATION/g, modTmpl.replace(/\$PID/g, p.id));
    } else {
      t = t.replace(/\$MODERATION/g, '');
    }
    if (href === 'favicon.ico') {
      return t.replace(/\$VIA/g, '').replace('via', '');
    }
    var img = "<a href='" + (p.original_source || '#') + "'>";
    if (brandId === "GooglePlus")
      img += "<img src = '" + href + "' height='16px' width='16px'/></a>";
    else
      img += "<img src = '" + href + "' height='24px' width='24px'/></a>";

    t = t.replace(/\$VIA/g, img);
    return t;
  },

  _renderList : function(data) {
    $('#pager').html('');
    $('#pager').append(UI.PAGER_HEAD_TEMPLATE);
    if (data.pages > 0) {
      var tmpl = '<li class="active"><span>$PAGENUM</span></li>';
      $('#pager').append(tmpl.replace(/\$PAGENUM/g, "Page " + UI.currentPage + " of " + data.pages));
    }

    if (UI.currentPage == 1) {
      $("#pagerp").addClass('disabled');
    } else {
      $("#pagerp").removeClass('disabled');
    }

    $('#pager').append(UI.PAGER_TAIL_TEMPLATE);

    if (UI.currentPage == data.pages || UI.currentPage == data.pages + 1) {
      $("#pagern").addClass('disabled');
    } else {
      $("#pagern").removeClass('disabled');
    }

    $('#predictions-list').html('');
    var tmpl = $("#pred-list-item-container").html();
    for ( var i = 0; i < data.results.length; i++) {
      var p = data.results[i];
      var html = UI.getPredictionHtml(tmpl, p);
      if (p.hasMore) {
        html = html.replace(/\$MAYBEMORE/g, UI.HAS_MORE_TEMPLATE.replace(/\$PID/g, p.id));
      } else {
        html = html.replace(/\$MAYBEMORE/g, "");
      }
      $(html).attr('id', 'pred-list-item-container-' + p.id);
      $('#predictions-list').append(html);
      UI.handleVotingFor(p.id);
    }
    window.scrollTo(0, 0);
  },

  loadPredictions : function() {
    this.apiClient.getPredictions(this.currentPage, this.PAGE_SIZE, {
      onSuccess : function(data, textStatus, jqXHR) {
        UI._renderList(data);
      },
      onError : function(jqXHR, textStatus, errorThrown) {
        $("#predictions-list").html("No Predictions");
        UI.showXHRError("Error loading predictions", jqXHR, textStatus, errorThrown);
      }
    });
  },
  _convertToSimpleArray : function(apiData){
    var facetField = apiData.facetField || '';
    var resultArray = [];
    for(var i = 0; i < apiData.results.length; i++){
      var dp = apiData.results[i];
      var key = dp.key.replace(facetField + '/', '');
      resultArray.push([key, dp.value]);
    }
    return resultArray;
  },
  
  _convertToLineChartData : function(apiData){
    var trendData = [['Day', 'All Predictions', 'Twitter', 'Facebook', 'GooglePlus', 'Web']];
    var facetField = apiData.ALL.facetField || '';
    for(var i = 0; i < apiData.ALL.results.length; i++){
      var dpAll = apiData.ALL.results[i];
      var dpTwitter = apiData.TWITTER.results[i];
      var dpFb = apiData.FACEBOOK.results[i];
      var dpGp = apiData.GOOGLEPLUS.results[i];
      var dpWeb = apiData.WEB.results[i];
      var key = dpAll.key.replace(facetField + '/', '');
      trendData.push([key, dpAll.value, dpTwitter.value, dpFb.value, dpGp.value, dpWeb.value]);
    }
    return trendData;
  },
  
  _convertToGeoChartData : function(apiData){
    var geoData = [['Country', 'Mentioned']];
    for(var i = 0; i < apiData.length; i++){
      var dp = apiData[i];
      geoData.push([dp.word, dp.freq]);
    }
    return geoData;
  },
  
  loadAnalytics : function() {
    $('#page-body').html($('#analytics-template').html());
    var days = 30, top = 10;
    var callbacks = {
        onSuccess: function(apiResponse, textStatus, jqXHR) {
          var pdata = apiResponse[0];
          var edata = apiResponse[1];
          var topSources = pdata['Top Sources'];
          var trend = pdata['Trend'];
          var geoCountries = edata.COUNTRY;
          UI._drawPieChart('Prediction Sources', UI._convertToSimpleArray(topSources), 'source-segmentation');
          UI._drawLineChart('Prediction Trends (for last ' + days + ' days)' , UI._convertToLineChartData(trend), 'trend-chart');
          UI._drawGeoChart('Countries Mentioned', UI._convertToGeoChartData(geoCountries), 'geo-mentions');
          UI._addItems(edata.TOPIC, 'top-topics');
          UI._addItems(geoCountries, 'geo-countries');
          UI._addItems(edata.SPORTS_EVENT, 'top-sports-events');
          UI._addItems(edata.INDUSTRY_TERM, 'top-industries');
          UI._addItems(edata.ORGANIZATION, 'top-org');
          UI._addItems(edata.COMPANY, 'top-company');
          UI._addItems(edata.PERSON, 'top-person');
        },
        onError : function(jqXHR, textStatus, errorThrown) {
          UI.showXHRError("Error loading analytics", jqXHR, textStatus, errorThrown);
        }
    };
    this.apiClient.getAnalytics(days, top, callbacks);
   
  },
  
  _addItems: function (data, element){
    var topicElem = $('#' + element);
    for(var i = 0; i < data.length; i++){
      var t = data[i];
      var topic = t.word.replace(/_/g, ' ');
      var val ='<div>' + topic + ' (' + t.freq + ')' + '</div>';
      $(topicElem).append(val);
    }
  },
  
  _drawGeoChart: function(title, data, element){
        var geochart = new google.visualization.GeoChart(document.getElementById(element));
        var options =  {title: title, width: 556, height: 347};
        geochart.draw(google.visualization.arrayToDataTable(data), options);
        $(window).resize(function(){
            geochart.draw(google.visualization.arrayToDataTable(data), options);
        });
  },
  
  _drawLineChart: function(title, data, element){
      var options = {curveType: "function",
               width: "100%", height: 300,
               chartArea:{left:100,top:50,width:"100%",height:"65%"},
               hAxis: {title : "Created On",showTextEvery: 10, format: 'd, MMM',slantedText: false,maxAlternation: 1, maxTextLines: 2, minTextSpacing: 5, titleTextStyle:{italic: false}},
               vAxis: {title: 'Count',viewWindow: {min: 0}},
               legend: {position: 'in', textStyle: {color: 'blue'}},
               pointSize: 2,
               title: title
        };
        var chart = new google.visualization.LineChart(document.getElementById(element));
        chart.draw(google.visualization.arrayToDataTable(data), options );
        $(window).resize(function(){
            chart.draw(google.visualization.arrayToDataTable(data), options );
        });
  },
  
  _drawPieChart : function(title, data, element){
    var chartData = new google.visualization.DataTable();
    chartData.addColumn('string', 'Source');
    chartData.addColumn('number', 'Predictions');
    chartData.addRows(data);

    // Set chart options
    var options = {
      'title' : title,
      titleTextStyle: {color: 'black', fontSize: 13},
      pieStartAngle: 290,
      'width' : 250,
      'height' : 300,
      'slices' : {
          1: {offset: 0.1},
          2: {offset: 0.1},
          3: {offset: 0.1},
       },
       legend : {
        position : 'top',
        'alignment' : 'center',
        maxLines: 2
       }

    };

    // Instantiate and draw our chart, passing in some options.
    var chart = new google.visualization.PieChart(document.getElementById(element));
    chart.draw(chartData, options);
  },
  
  handleVotingFor : function(pid) {
    if (!UI.isLoggedIn) {
      UI.disableVoting(pid);
    } else if (UI.currentUser) {
      for (e in UI.currentUser.votedUp) {
        if (pid === UI.currentUser.votedUp[e] || pid === UI.currentUser.votedDown[e]) {
          UI.disableVoting(pid);
        }
      }
    }
  },

  showXHRError : function(message, jqXHR, textStatus, errorThrown) {
    if (jqXHR.responseJSON) {
      var m = "";
      if (jqXHR.responseJSON.errors) {
        for ( var i = 0; i < jqXHR.responseJSON.errors.length; i++) {
          var e = jqXHR.responseJSON.errors[i];
          m = m + " " + e.message;
        }
      }
      this.showGlobalError(jqXHR.status + " : " + message + " : " + m);
    } else {
      this.showGlobalError(jqXHR.status + " : " + message + " : " + jqXHR.responseText);
    }
  },

  showGlobalMessage : function(msg) {
    this._showMessage('#global-info-message', msg);
  },

  showGlobalError : function(msg) {
    this._showMessage('#global-error-message', msg, 10000);
  },

  showGlobalWarning : function(msg) {
    this._showMessage('#global-warning-message', msg, 10000);
  },

  _showMessage : function(id, message, timeout) {
    var tmpl = $(id).html();
    tmpl = tmpl.replace(/\$MSG/g, message);
    $("#messages").html('');
    $("#messages").append(tmpl);
    if (!$("#messages").is(":visible")) {
      $("#messages").fadeTo(0, 100);
      window.setTimeout(function() {
        $("#messages").fadeTo(500, 0).slideUp(500, function() {
          $(id).hide();
        });
      }, timeout || 5000);
    }

  },

  loadPrediction : function(id) {
    $('#page-body').html($('#pid-loader').html());
    this.apiClient.getPrediction(id, {
      onSuccess : function(data, textStatus, jqXHR) {
        var p = data;
        var tmpl = $("#predictions-container").html();
        tmpl = UI.getPredictionHtml(tmpl, p);
        $(tmpl).attr('id', 'predictions-container_' + p.id);
        $('#page-body').html(tmpl);
        if (p.comments) {
          var commentContainer = $("#comments-list-" + p.id);
          commentContainer.html('');
          var ctmpl = $('#comment-container').html();
          for ( var i = 0; i < p.comments.length; i++) {
            var c = p.comments[i];
            var chtml = ctmpl.replace(/\$USER/g, c.author).replace(/\$COMMENTTEXT/g, c.comment);
            commentContainer.append(chtml);
          }
        }
        $('#captcha-label').html(data.captcha);
        $('#captcha-label').attr('data-value', data.captchaId);
        $("#page-body").effect("highlight", {
          color : "#ffff99"
        }, 300);
        UI.schedule(100, function() {
          UI.handleVotingFor(p.id);
        });
      },
      onError : function(jqXHR, textStatus, errorThrown) {
        console.log(jqXHR);
      }
    });
  },

  createPrediction : function(elements) {
    $('#loader-createPrediction').show();
    var obj = {};
    $.each(elements, function(i, e) {
      var at = $(e).attr('id').replace('prediction-', '');
      if (at === 'time' && $(e).val().length > 0) {
        obj[at] = new Date($(e).val());
      } else
        obj[at] = $(e).val();
    });
    var successFunction = function(data, textStatus, jqXHR) {
      $('#messages').html('');
      $('#cerrors').html('');
      $('#makePred').modal('hide');
      $('#quotePred').modal('hide');
      $.each(elements, function(i, e) {
        if ($(e).attr('type') !== 'hidden') {
          $(e).val(' ');
          $(e).parent().removeClass('has-error');
        }
      });
      UI.showGlobalMessage("Prediction created");
      $('#loader-createPrediction').hide();
      UI.loadPredictions();
    };
    var errorFn = function(jqXHR, textStatus, errorThrown) {
      $('#loader-createPrediction').hide();
      UI._handleValidationError(jqXHR, "#prediction", function() {
        $('#cerrors').append(jqXHR.responseText);
      });
    };
    this.apiClient.createPrediction(obj, {
      onSuccess : successFunction,
      onError : errorFn,
    });
  },

  _handleValidationError : function(jqXHR, prefix, or) {
    if (jqXHR.status) {
      var validationErrors = jqXHR.responseJSON.errors || [];
      for (e in validationErrors) {
        var field = validationErrors[e].field;
        var ph = UI.ERRORS[validationErrors[e].type];
        UI._highlightValidationError(prefix, field, ph);
      }
    } else {
      or();
    }
  },

  createComment : function(pid, captchaId, captcha, author, comment) {
    var commentObject = {
      captchaId : captchaId,
      captcha : captcha,
      author : author,
      comment : comment
    };
    var callbacks = {
      onSuccess : function(data, textStatus, jqXHR) {
        var controls = $('*[id^=\"comment-\"]');
        for (c in controls) {
          try {
            c.val('');
          } catch (e) {
          }
        }
        UI.scheduleReload(100);
      },

      onError : function(jqXHR, textStatus, errorThrown) {
        UI._handleValidationError(jqXHR, "#comment", function() {
          UI.showXHRError("Error creating comment: ", jqXHR, textStatus, errorThrown);
        });
      }
    };
    this.apiClient.createComment(pid, commentObject, callbacks);
  },

  scheduleReload : function(timeout) {
    UI.schedule(timeout, function() {
      location.reload();
    });
  },

  schedule : function(timeout, fn) {
    setTimeout((function() {
      fn();
    }), timeout || 1000);
  },

  signup : function() {
    var e = $('#user-email').val();
    var pw = $('#user-pass').val();
    var captchaId = $("#user-captcha-label").attr('data-value');
    var captcha = $("#user-captcha").val();

    if (UI._invalidInput(e)) {
      UI._highlightValidationError("#user", "email", UI.ERRORS["required"]);
      return;
    }
    if (UI._invalidInput(pw)) {
      UI._highlightValidationError("#user", "pass", UI.ERRORS["required"]);
      return;
    }
    if (UI._invalidInput(captcha)) {
      UI._highlightValidationError("#user", "captcha", UI.ERRORS["required"]);
      return;
    }
    $('#loader-signup').show();
    var callbacks = {
      onSuccess : function(data, textStatus, jqXHR) {
        $('#loader-signup').hide();
        UI.scheduleReload();
      },

      onError : function(jqXHR, textStatus, errorThrown) {
        $('#loader-signup').hide();
        UI._handleValidationError(jqXHR, "#user", function() {
          UI.showXHRError("Error creating user:", jqXHR, textStatus, errorThrown);
        });
      }
    };
    this.apiClient.createUser({
      captchaId : captchaId,
      captcha : captcha,
      email : e,
      pass : pw
    }, callbacks);
  },

  signin : function() {
    var email = $('#user-login-email').val();
    var pw = $('#user-login-pass').val();
    if (UI._invalidInput(email)) {
      UI._highlightValidationError("#user-login", "email", UI.ERRORS["required"]);
      return;
    }
    if (UI._invalidInput(pw)) {
      UI._highlightValidationError("#user-login", "pass", UI.ERRORS["required"]);
      return;
    }
    var callbacks = {
      onSuccess : function(data, textStatus, jqXHR) {
        $('#loader-login').hide();
        $('#login').modal('hide');
        UI.scheduleReload();
      },

      onError : function(jqXHR, textStatus, errorThrown) {
        $('#loader-login').hide();
        UI._handleValidationError(jqXHR, "#user-login", function() {
          UI.showXHRError("Error logging in :", jqXHR, textStatus, errorThrown);
        });
      }
    };
    $('#loader-login').show();
    UI.apiClient.login({
      email : email,
      pass : pw
    }, callbacks);
  },

  _highlightValidationError : function(prefix, field, placeHolder) {
    if (field) {
      $(prefix + "-" + field).parent().addClass('has-error');
      $(prefix + "-" + field).attr('placeholder', placeHolder);
      $(prefix + "-" + field).effect("highlight", {
        color : "#662222"
      }, 500);
    }
  },

  _invalidInput : function(str) {
    return !str || str.trim().length == 0;
  },

  voteUp : function(id) {
    this.vote(id, 'up');
  },
  voteDown : function(id) {
    this.vote(id, 'down');
  },

  disableVoting : function(id, increment) {
    var arr = [ 'up', 'down' ];
    for ( var i = 0; i < 2; i++) {
      var type = arr[i];
      var e = $('#' + type + '_' + id);
      var splitArray = e.html().split(' ');
      var label = splitArray[splitArray.length - 2];
      var newVal = splitArray[splitArray.length - 1];
      var count = parseInt(newVal.match(/\(.*\)/)[0].replace('(', '').replace(')', ''));
      if (increment && increment === type)
        count++;
      e.html(label + " (" + count + ")");
      e.removeAttr('onclick');
      e.addClass('disabled');
    }
  },

  vote : function(id, type) {
    var callbacks = {
      onSuccess : function(data, textStatus, jqXHR) {
        UI.disableVoting(id, true);
      },
      onError : function(jqXHR, textStatus, errorThrown) {
        if (!UI.isLoggedIn) {
          $('#user-login').modal('show');
        } else {
          UI.showXHRError("Error voting " + type, jqXHR, textStatus, errorThrown);
        }
        UI.disableVoting(id, false);
      }
    };
    if (type === 'up') {
      this.apiClient.voteUp(id, callbacks);
    } else {
      this.apiClient.voteDown(id, callbacks);
    }
    ;
  },

  search : function() {
    var q = $('#tsearch').val();
    if (q.trim().length == 0) {
      UI.loadPredictions();
      return;
    }
    var callbacks = {
      onSuccess : function(data, textStatus, jqXHR) {
        $('#mainpage-title').text("Search results for '" + q + "'");
        UI._renderList(data);
      },
      onError : function(jqXHR, textStatus, errorThrown) {
        UI.showXHRError("Error searching: " + q, jqXHR, textStatus, errorThrown);
      }
    };
    this.apiClient.search(q, callbacks);
  },

  approve : function(elem, predictionId) {
    this.internalModerate(elem, 'approve', predictionId);
  },

  reject : function(elem, predictionId) {
    this.internalModerate(elem, 'reject', predictionId);
  },

  internalModerate : function(elem, type, predictionId) {
    var callbacks = {
      onSuccess : function(data, textStatus, jqXHR) {
        if (!window.location.hash) {
          UI.remaining--;
          if (UI.remaining <= 0) {
            UI.loadPredictionsPage(1);
            UI.remaining = UI.PAGE_SIZE;
          }
        } else {
          window.location.replace("/");
        }

        $(elem).parent().parent().parent().parent().parent().fadeTo(100, 0).slideUp(200, function() {
          UI.showGlobalMessage("Prediction with id: " + predictionId + " - " + data.message);
        });
      },
      onError : function(jqXHR, textStatus, errorThrown) {
        UI.showXHRError("Error moderating '" + type + "' :" + q, jqXHR, textStatus, errorThrown);
      }
    };
    if (type === 'approve') {
      this.apiClient.approve(predictionId, callbacks);
    } else {
      this.apiClient.reject(predictionId, callbacks);
    }
  }

};

var UI = new MPFront();
