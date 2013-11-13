<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="mainWithNav"/>
    <meta name="menu-level-1" content="storage"/> 
    <meta name="menu-level-2" content="snapshots"/> 
    <meta name="menu-level-3" content="Create Volume Snapshot"/> 
    <title>Create Volume Snapshot</title>
    
  </head>

  <body>
    <div class="body">
      <div class="container">
        <div class="row">
          <div class="col-md-12">
            <g:if test="${flash.message}">
              <div id="message" class="alert alert-info">${flash.message}</div>
            </g:if>          
            <g:hasErrors bean="${cmd}">
              <div id="error_message" class="alert alert-error">
                <g:renderErrors bean="${cmd}" as="list"/>
              </div>
            </g:hasErrors>
          </div>
        </div>
        <g:form method="post" class="validate form-horizontal fill-up">
          <input type="hidden" id="id" name="id" value="${params.id}"/>
          <div class="box">
            <div class="box-header">
              <span class="title">Create Volume Snapshot</span>
            </div>
            <div class="box-content padded">
              <div class="form-group">
                <label class="control-label col-lg-2 required">Name *</label>
                <div class="col-lg-4">
                  <g:textField id="name" name="name" value="${params.name}"/>
                </div>
              </div> 
              <div class="form-group">
                <label class="control-label col-lg-2 required">Description *</label>
                <div class="col-lg-4">
                  <g:textField id="description" name="description" value="${params.description}"/>
                </div>
              </div>   

              <div class="form-actions">
                <g:buttonSubmit class="btn btn-green" id="submit" action="save" title="Create snapshot">Create Volume Snapshot</g:buttonSubmit>
              </div>
            </div>
          </div>
        </g:form>
      </div>
    </div>
  </body>
</html>
