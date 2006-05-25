
<html>
    <head>
         <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
		 <meta name="layout" content="${session.site.domain}" />
         <title>My Profile</title>
    </head>
    <body>
        <div class="body">
           <g:if test="${flash['message']}">
                 <div class="message">${flash['message']}</div>
           </g:if>
           <g:hasErrors bean="${user}">
                <div class="errors">
                    <g:renderErrors bean="${user}" as="list" />
                </div>
           </g:hasErrors>
           <g:form action="updateProfile" method="post">
		   		<input type="hidden" name="id" value="${user.id}" />
               <div class="dialog">
                <table class="userForm">
                       <tr class='prop'>
					   		<td valign='top' style='text-align:left;' width='20%'>
								<label for='login'>Login:</label>
							</td>
							<td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:user,field:'login','errors')}'>
								${user?.login}
							</td>
						</tr>
						
                       <tr class='prop'>
					   		<td valign='top' style='text-align:left;' width='20%'>
								<label for='login'>Password:</label>
							</td>
							<td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:user,field:'login','errors')}'>
								<input type='password' name='pwd' value='${user?.pwd}' />
							</td>
						</tr>		
						
						  <tr class='prop'><td valign='top' style='text-align:left;' width='20%'><label for='title'>Title:</label></td><td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:user,field:'title','errors')}'><input type='text' name='title' value='${user?.title}' /></td></tr>

						  <tr class='prop'><td valign='top' style='text-align:left;' width='20%'><label for='firstName'>First Name:</label></td><td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:user,field:'firstName','errors')}'><input type='text' name='firstName' value='${user?.firstName}' /></td></tr>
			   
						  <tr class='prop'><td valign='top' style='text-align:left;' width='20%'><label for='lastName'>Last Name:</label></td><td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:user,field:'lastName','errors')}'><input type='text' name='lastName' value='${user?.lastName}' /></td></tr>
						  
						  <tr class='prop'><td valign='top' style='text-align:left;' width='20%'><label for='company'>Company:</label></td><td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:user,field:'company','errors')}'><input type='text' name='company' value='${user?.company}' /></td></tr>
			   
						  <tr class='prop'><td valign='top' style='text-align:left;' width='20%'><label for='email'>Email:</label></td><td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:user,field:'email','errors')}'><input type='text' name='email' value='${user?.email}' /></td></tr>
						  
						  <tr class='prop'><td valign='top' style='text-align:left;' width='20%'><label for='jobTitle'>Job Title:</label></td><td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:user,field:'email','errors')}'><input type='text' name='jobTitle' value='${user?.jobTitle}' /></td></tr>
						  
                     <tr class='prop'>
					   		<td valign='top' style='text-align:left;' width='20%'>
								<label for='login'>Employment Status:</label>
							</td>
							<td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:user,field:'employmentStatus','errors')}'>
								<g:select name="employmentStatus" value="${user.employmentStatus}"
																	from="[	'Job Searching',
																			'Full time',
																			'Part time',
																			'Self-employed',
																			'Interim',
																			'Consultant']" />
							</td>
						</tr>
                     <tr class='prop'>
					   		<td valign='top' style='text-align:left;' width='20%'>
								<label for='login'>Job Function:</label>
							</td>
							<td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:user,field:'jobFunction','errors')}'>
								<g:select name="jobFunction" value="${user.jobFunction}"
											from="[ 'Finance and Accountancy',
													'Marketing/Media',
													'IT',
													'HR',
													'Operations',
													'Strategy',
													'Business Development',
													'Sales',
													'Engineering',
													'PR/Communications',
													'Legal',
													'Procurement/Purchasing',
													'Manufacturing/Production',
													'Project Management',
													'Logistics/Facilities',
													'Consultancy',
													'Customer Services',
													'Compliance/Regulations',
													'Logistics Facilities',
													'Manager',
													'Director/CEO',
													'Other']" />
							</td>
						</tr>								
                     <tr class='prop'>
					   		<td valign='top' style='text-align:left;' width='20%'>
								<label for='login'>Job Level:</label>
							</td>
							<td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:user,field:'jobLevel','errors')}'>
								<g:select name="jobLevel" value="${user.jobLevel}" 
								from="['Consultant','Middle Management', 'Non-Executive Director', 'Senior Executive (CEO/MD/CFO/Chairman)', 'Senior Manager', 'Self-Employed']" />
							</td>
						</tr>
                     <tr class='prop'>
					   		<td valign='top' style='text-align:left;' width='20%'>
								<label for='login'>Industry Sector:</label>
							</td>
							<td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:user,field:'industrySector','errors')}'>
								<g:select name="industrySector" value="${user.industrySector}"
											from="[ 'Aerospace',
													'Agriculture',
													'Automotive',
													'Construction',
													'Chemical',
													'Cosmetics',
													'Consultancy/Professional Services',
													'Defence',
													'Education',
													'Electronics',
													'Engineering',
													'Finance - Accountancy, Tax and Treasure',
													'Finance - Banking and Insurance',
													'Food and Drink',
													'FMCG',
													'Government',
													'Healthcare',
													'Hospitality',
													'HR (Human Resources)',
													'IT',
													'Legal',
													'Leisure',
													'Manufacturing',
													'Media',
													'Oil and Gas',
													'Not for Profit',
													'Pharmaceuticals',
													'Property',
													'Retail',
													'Services',
													'Telecommunications',
													'Transport',
													'Travel/Tourism',
													'Utilities',
													'Other']" />
							</td>
						</tr>	
						
                     <tr class='prop'>
					   		<td valign='top' style='text-align:left;' width='20%'>
								<label for='login'>Location:</label>
							</td>
							<td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:user,field:'location','errors')}'>
								<g:select name="location" value="${user.location}"
											from="[ 'UK - Greater London',
													'UK - Thames Valley',
													'UK - Midlands',
													'UK - North East',
													'UK - North West',
													'UK - Devon & Cornwall',
													'UK - South England',
													'UK - North England',
													'UK - Kent',
													'UK - Essex',
													'UK - Northern Ireland',
													'UK - Scotland',
													'UK - Wales',
													'Ireland',
													'Europe',
													'USA',
													'Asia',
													'Australia',
													'Africa',
													'Canada',
													'South America',
													'Middle East']" />
							</td>
						</tr>							
               </table>
               </div>
               <div class="buttons">
                     <span class="formButton">
                        <input type="submit" value="Update"></input>
                     </span>
               </div>
            </g:form>
        </div>
    </body>
</body>
            
