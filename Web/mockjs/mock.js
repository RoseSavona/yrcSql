Mock.mock("/api/login",function(){
	var data = Mock.mock({
		"loginCode|0-2":1,
	})
	if(data["loginCode"]!=0){
		$.cookie("userID",Mock.Random.string(16));
		$.cookie("username",Mock.Random.cname());
		$.cookie("character","student");
	}
	return JSON.stringify(data);
});

Mock.mock("/api/logout",function(){
	$.removeCookie("userID");
	$.removeCookie("username");
	$.removeCookie("character");
});

Mock.mock("/api/examStart",function(){
	var data = Mock.mock({
		"examStart|1":1,
	});
	return JSON.stringify(data);
});

Mock.mock("/api/changePassword",function(){
	return true;
});

Mock.mock("/api/submitExam",function(Jans){
	var ans = JSON.parse(Jans.body);
	//console.log(JSON.parse(Jans.body));
	ans.data=new Array();
	for(i=0;i<3;i++){
		for(j=0;j<ans.Qtype[i];j++){
			if(i==0){
				var num=[1,2,4,8];
				ans["data"].push(num[Mock.Random.integer(0,3)]);
			}else if(i==1){
				ans["data"].push(Mock.Random.integer(1,15));
			}else{
				ans["data"].push(num[Mock.Random.integer(0,1)]);
			}
		}
	}

	//console.log(ans);
	if($.cookie("examType")==0){
		return null;
	}else{
		return JSON.stringify(ans);
	}
});

Mock.mock("/api/getExam",function(){
	var date = new Date();
	date.setSeconds(date.getSeconds()+15*60);

	var ts= date.getFullYear()+'/'+(date.getMonth()+1)+'/'+date.getDate()+' ';
	ts+=date.getHours()+':'+date.getMinutes()+':'+date.getSeconds();

	var id =Mock.Random.string(16);
	var Qnum = [10,20,20]
	//id="same"
	var data = {
		examID:id,
		date:ts,
		Qtype:Qnum
	}
	var Darr = new Array();
	for(i=0;i<3;i++){
		for(j=0;j<Qnum[i];j++){
			var sDataTmp={};
			sDataTmp["title"]=Mock.Random.cparagraph();
			var tip = new Array();
			if(i<2){
				for(z=0;z<4;z++){
					tip.push(Mock.Random.csentence());
				}
				sDataTmp["choice"]=tip;
			}
			Darr.push(sDataTmp);
		}
	}
	data["data"]=Darr;
	
	return JSON.stringify(data);
});
