<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="stylesheet" href="//netdna.bootstrapcdn.com/bootstrap/3.0.0/css/bootstrap.min.css">
    <link rel="stylesheet" href="//netdna.bootstrapcdn.com/bootstrap/3.0.0/css/bootstrap-theme.min.css">
    <meta name="description" content="${model.prediction.text}">
    <title>${model.prediction.title} - Predictions Central</title>
    <link rel="stylesheet" href="/css/mp.css" />
    <!-- HTML5 shim and Respond.js IE8 support of HTML5 elements and media queries -->
    <!--[if lt IE 9]>
    <script src="../../assets/js/html5shiv.js"></script>
    <script src="../../assets/js/respond.min.js"></script>
    <![endif]-->
</head>
<body>
<div class="navbar navbar-fixed-top navbar-active navbar-inverse">
    <div class="container">
        <div class="navbar-header">
            <button type="button" class="navbar-toggle" data-toggle="collapse" data-target=".navbar-collapse">
                <span class="icon-bar"></span> <span class="icon-bar"></span> <span class="icon-bar"></span>
            </button>
            <a class="navbar-brand" href="/">Prediction Central</a>
        </div>
        <div class="collapse navbar-collapse ">
            <ul class="nav navbar-nav ">
                <li class="active"><a href="#new">New</a></li>
                <!--<li><a href="#top">Top</a></li>-->
            </ul>
            <div class="navbar-form navbar-right" style="display: none" id="signin-area">
                <a data-toggle="modal" href="#signup" class="btn btn-danger">Sign up</a>
                <a data-toggle="modal" class="btn" href="#login" style="color: white; text-decoration: underline;">Sign In</a>
            </div>
            <p class="navbar-text pull-right" style="display: none" id="signedas-area">
                Signed in as <a href="#" class="navbar-link">$USER</a>
            </p>
            <div class="navbar-form navbar-left">
                <a data-toggle="modal" href="#makePred" class="btn-primary btn">Predict!</a>
            </div>
            <div class="col-sm-3 col-md-3 pull-right">
                <form class="navbar-form" role="search">
                    <div class="input-group">
                        <input type="text" class="form-control" placeholder="Search" name="srch-term" id="tsearch">
                        <div class="input-group-btn">
                            <button class="btn btn-default" onclick="UI.search();return false;"><i class="glyphicon glyphicon-search"></i></button>
                        </div>
                    </div>
                </form>
            </div>
        </div>
        <!-- /.nav-collapse -->
    </div>
    <!-- /.container -->
</div>
<!-- /.navbar -->
<div class="container">
<div class="row row-offcanvas row-offcanvas-right">
    <div class="col-xs-12 col-sm-9" id='page-body'>
        <div id="messages"></div>
            <div id="predictions-container">
                <div class="panel panel-default">
                    <div class="panel-heading">
                        <h3 class="panel-title">${model.prediction.title}</h3>
                    </div>
                    <div class="panel-body">
                        <blockquote>
                            <p>${model.prediction.time?number_to_date}</p>
                            <div >&#x275d; <pre>${model.prediction.text}</pre> &#x275e;</div>

                            <small class="text-right">
                                <a href="#">
                                    <#if model.prediction.sourceAuthor??>
                                        ${model.prediction.sourceAuthor}
                                    <#else>
                                        ${model.prediction.createdByUser}
                                    </#if>
                                </a>
                                <cite title="Source Title"> predicted</cite> on ${model.prediction.createTimestamp?number_to_date}
                            </small>
                        </blockquote>
                        <div>
                            <div class="pull-left">
                                <div class="btn likely pull-left" onclick="javascript:UI.voteUp(${model.prediction.id})" id="up_${model.prediction.id}">&#9650; Likely (${model.prediction.upvotes})</div>
                                <div class="btn unlikely pull-left" onclick="javascript:UI.voteDown(${model.prediction.id})" id="down_${model.prediction.id}">&#9660; Unlikely (${model.prediction.downvotes})</div>
                            </div>
                        </div>
                        <div class="pull-right">${model.prediction.tags}</div>
                        <br>
                        <hr>
                        <div>
                            <h4 class="">Comments:</h4>
                        </div>
                        <div class="panel-body" id="comments-list-$PID">
                            <div class="media" id="comment-container">
                            <#list model.prediction.comments as comment>
                                    <ul class="media-list list-group" id="prediction_comments">
                                        <li class="media list-group" style="padding: 10px">
                                            <div class="media-body">
                                                <div class="media-heading">
                                                    <b>${comment.author}</b> said:
                                                </div>
                                                <div id="comment-text">${comment.comment}</div>
                                            </div>
                                        </li>
                                    </ul>
                            </#list>
                            </div>
                            <#if model.prediction.comments?size < 1>
                                <div>Be the first to comment!</div>
                            </#if>
                        </div>
                        <hr>
                        <div>Post a comment</div>
                        <#if model.currentUser.registered>
                            <form class="form" style="width: 70%; padding: 5px">
                                <div class="form-group">
                                    <input type="text" class="form-control" id="comment-author" disabled=disabled value="${model.currentUser.name}">
                                </div>
                                <div class="form-group">
                                    <textarea rows="4" cols="80" id="comment-comment" class="form-control" placeholder="Comment (Required)"></textarea>
                                </div>
                                <button type="button" class="btn btn-success"
                                        onclick="UI.createComment(${model.prediction.id}, 0, '', $('#comment-author').val(), $('#comment-comment').val());">Post comment</button>
                            </form>
                        <#else>
                            <form class="form" style="width: 70%; padding: 5px">
                                <div class="form-group">
                                    <input type="text" class="form-control" id="comment-author" placeholder="Name (Optional)" value="${model.currentUser.fullName}">
                                </div>
                                <div class="form-group">
                                    <textarea rows="4" cols="80" id="comment-comment" class="form-control" placeholder="Comment (Required)"></textarea>
                                </div>
                                <div class="form-group form-inline">
                                    <label for="comment-captcha" id="captcha-label" data-value='${model.prediction.captchaId?c}'>${model.prediction.captcha} </label> <input type="text" class="form-control" id="comment-captcha"
                                                                                                                                                                             placeholder="Prove you are human by answering a simple math question above">
                                </div>
                                <button type="button" class="btn btn-success"
                                        onclick="UI.createComment(${model.prediction.id}, $('#captcha-label').attr('data-value'),  $('#comment-captcha').val(), $('#comment-author').val(), $('#comment-comment').val());">Post comment</button>
                            </form>
                        </#if>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
<!--/row-->
<hr>
<!-- /.modal-dialog-prediction -->
<div class="modal fade" id="makePred">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal">&times;</button>
                <h4 class="modal-title">What do you want to predict?</h4>
            </div>
            <div class="modal-body">
                <form class="form">
                    <span>Use following format</span>
                    <blockquote>
                        <div class="well well-sm">&#x275d; By | In <b>YEAR</b>, Prediction text &#x275e;</div>
                        <small class="text-right">
                            Author, Date, Tag1, Tag2 ...
                        </small>
                    </blockquote>
                    <div class="form-group ">
                        <div class="well well-sm">&#x275d;<textarea rows="10" cols="80" id="prediction-text" class="form-control" placeholder="Enter your prediction..."></textarea>&#x275e;

                        </div>
                        <div class="text-right">
                            <small class="text-right" style="text-align: right">
                                --
                                <input id="prediction-originalAuthor" class="prediction-input" type="text" placeholder="Author">
                                <input id="prediction-time" class="prediction-input" type="text" placeholder="Date">
                                <input id="prediction-tags" class="prediction-input" type="text" placeholder="Tags">
                            </small>
                        </div>
                    </div>
                </form>
            </div>
            <div class="modal-footer">
                        <span id="loader-createPrediction" style="display: none; ">
                         Creating Prediction, Please wait... <img src="/images/loader.gif"  >
                        </span>
                <button type="button" class="btn btn-success" onclick='UI.createPrediction($("*[id^=\"prediction-\"]"));'>Record my prediction!</button>
            </div>
            <div id='cerrors'></div>
        </div>
        <!-- /.modal-content -->
    </div>
</div>
<!-- /.modal-dialog-prediction -->
<!-- /.modal -->
<!-- Templates TODO: Use template tag -->
<!-- Prediction Template -->
<div style="display: none">
    <div id="global-info-message">
        <div class="alert alert-success alert-dismissable" id="info">
            <button type="button" class="close" data-dismiss="alert">&times;</button>
            <div>$MSG</div>
        </div>
    </div>
    <div id="global-error-message">
        <div class="alert alert-danger alert-dismissable" id="error">
            <button type="button" class="close" data-dismiss="alert">&times;</button>
            <div>$MSG</div>
        </div>
    </div>
    <div id="global-warn-message">
        <div class="alert alert-warning alert-dismissable" id="warning">
            <button type="button" class="close" data-dismiss="alert">&times;</button>
            <div>$MSG</div>
        </div>
    </div>

</div>
<!-- Signin template -->
<div class="modal fade" id="signup">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal">&times;</button>
                <h4 class="modal-title">Sign up</h4>
                <p>
                    By creating your Prediction Central account you agree to our <a href="#">Terms of Use</a> and <a href="#">Privacy Policy</a>.
                </p>
            </div>
            <div class="modal-body">
                <form>
                    <div class="form-group">
                        <input type="text" class="form-control" id="user-name" placeholder="Display Name (Optional)" required="required">
                    </div>
                    <div class="form-group">
                        <input type="text" class="form-control" id="user-email" placeholder="Enter email (Required)" required="required">
                    </div>
                    <div class="form-group">
                        <input type="password" class="form-control" id="user-pass" placeholder="Password (Required)" required="required">
                    </div>
                    <div class="form-group">
                        <label for="user-captcha" id="user-captcha-label" data-value=''>$EXPRSTR = ?</label>
                        <input type="text" class="form-control" id="user-captcha" placeholder="Prove you are human by answering a simple math question above">
                    </div>
                    <div class="checkbox">
                        <div>
                            <div class="pull-left">
                                <label> <input type="checkbox" checked="checked"> Remember me
                                </label>
                            </div>
                            <div class="pull-right">
                                        <span id="loader-signup" class="pull-left" style="display: none;"> Creating your account, Please wait... <img src="/images/loader.gif">
                                        </span>
                                <button class="pull-right btn btn-primary" onclick="javascript:UI.signup();return false;">Sign up</button>
                            </div>
                        </div>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>
<!-- End signin template -->
<!-- Login Template -->
<div class="modal fade" id="login">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close" data-dismiss="modal">&times;</button>
                <h4 class="modal-title">Log In</h4>
            </div>
            <div class="modal-body">
                <form>
                    <div class="form-group">
                        <input type="email" class="form-control" id="user-login-email" placeholder="Email" required="required">
                    </div>
                    <div class="form-group">
                        <input type="password" class="form-control" id="user-login-pass" placeholder="Password" required="required">
                    </div>
                    <div class="checkbox">
                        <div>
                            <div class="pull-left">
                                <label> <input type="checkbox" checked="checked"> Remember me
                                </label>
                            </div>
                            <div class="pull-right">
                                        <span id="loader-login" class="pull-left" style="display: none;"> Logging in, Please wait... <img src="/images/loader.gif">
                                        </span>
                                <button class="pull-right btn btn-primary" onclick="javascript:UI.signin();return false;">Log In</button>
                            </div>
                        </div>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>
<!-- End Login -->
<footer>
    <div class="pull-left">
        <span style="padding-right: 20px" >Registered Users: <b id="ucount">$USERS</b> </span> |
        <span style="padding-left: 20px; padding-right: 20px" >Visitors: <b id="vcount">$VISITORS</b> </span>
    </div>
    <div class="pull-right">
        <span>&copy; Nirav Thaker 2013</span>
    </div>

</footer>
</div>
<!--/.container-->
<script src="//code.jquery.com/jquery.min.js"></script>
<script src="//netdna.bootstrapcdn.com/bootstrap/3.0.0/js/bootstrap.min.js"></script>
<script src="/js/mp.js"></script>
<script type="text/javascript">
    $(document).ready(function() {
    UI.init(true);
    });
</script>
<script>
    (function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
    (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
    m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
    })(window,document,'script','//www.google-analytics.com/analytics.js','ga');

    ga('create', 'UA-44534204-1', 'prediction-central.com');
    ga('send', 'pageview');

</script>
</body>
</html>


