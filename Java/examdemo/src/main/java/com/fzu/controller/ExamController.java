package com.fzu.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fzu.pojo.*;
import com.fzu.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.ParseException;
import java.util.*;

@Controller
public class ExamController {
    @Autowired
    StudentService studentService=new StudentServiceImpl();
    @Autowired
    AdminService adminService=new AdminServiceImpl();
    @Autowired
    TeacherService teacherService=new TeacherServiceImpl();

    //为批量的班级添加考试
    @RequestMapping("/setClassInExam")
    @ResponseBody
    public Map<String,Integer> setClassInExam(@RequestBody List<ClassExam> classExamList){
        Map<String,Integer> result=new HashMap<>();
        for(int i=0;i<classExamList.size();i++){
            ClassExam classExam;
            classExam=classExamList.get(i);
            teacherService.updateClassExam(classExam);
            System.out.println(i+classExam.toString());
        }
        result.put("status",1);
        return result;
    }
    //将所有班级设置为正式考试
    @RequestMapping("/setOfficialExam")
    @ResponseBody
    public Map<String,Integer> setOfficialExam(@RequestBody JSONObject jsonObject){
        String examTime=jsonObject.getString("examTime");
        System.out.println("examTime");
        Map<String,Integer> result=new HashMap<>();
        adminService.setOfficialExam(examTime);
        result.put("status",1);
        return result;
    }
    @RequestMapping("/closeOfficialExam")
    @ResponseBody
    public Map<String,Integer> closeOfficialExam(){
        adminService.closeOfficialExam();
        Map<String,Integer> result=new HashMap<>();
        result.put("status",1);
        return result;
    }


    //获得班级考试信息(列表)
    @RequestMapping("/getClassInExam")
    @ResponseBody
    public List<ClassExam> getClassInExam(HttpServletRequest request){
        List<ClassExam> classExamList=new ArrayList<ClassExam>();
        //获取某个老师管理的班级的考试信息(需要从cookie中拿出教师id)
        String teacher_id="";
        Cookie[] cookies=request.getCookies();
        for(Cookie i:cookies){
            if(i.getName().equals("username")) teacher_id=i.getValue();
        }
        classExamList=teacherService.getClassExamList(teacher_id);
        return classExamList;
    }

    //返回班级的考试是否开始的状态
    @RequestMapping("/examStatus")
    @ResponseBody
    public Map<String,Integer> examStatus(HttpServletRequest request,@RequestBody JSONObject jsonObject){
        Map<String,Integer> result=new HashMap<>();
        Integer examType=jsonObject.getInteger("examType");//0代表正式，1代表模拟 1->2 2->1
        int temp = -1;
        if(examType==1)
            temp = 1;
        else if(examType==0)
            temp = 2;
        else
            System.out.println("出错的examType");

        examType = temp;
        String studentId="";
        Cookie[] cookies=request.getCookies();
        for(Cookie i:cookies){
            if(i.getName().equals("username")) studentId=i.getValue();
        }
        //通过学生id得到班级id，通过班级id获取examstatus。
        Integer classId=studentService.getClassId(studentId);
        ClassExam classExam=studentService.getClassExam(classId);
        //如果为0则为未开始
        if(classExam.getClassStatus()==0)
           result.put("examStart",0);
        //如果非0，通过班级id获取考试开始时间。考试开始时间未到为未开始
        else{
            Date startTime=studentService.getStarttime(classId);
            Date now=new Date();
            System.out.println(examType+"--"+classExam.getClassStatus());
            boolean isSame = examType==classExam.getClassStatus();
            if(now.before(startTime)) result.put("examStart",0);
            else if (isSame) result.put("examStart",1);
            else {
                System.out.println("examStatus isn't same");
                result.put("examStart",0);
            }
        }
        return result;
    }

    //获得考试卷
    @RequestMapping("/getPaper")
    @ResponseBody
    public ExamPaper getPaper(HttpServletRequest request,HttpServletResponse response) throws ParseException {
        Cookie[] cookies=request.getCookies();
        int flag=0;
        Integer paperId=0;
        for (Cookie i:cookies){
            if (i.getName().equals("paperId")){
                paperId=Integer.valueOf(i.getValue());
                flag=1;
                break;
            }
        }
        //如果paperId存在，试卷已存在，返回已有的卷子
        if(flag==1){
            System.out.println("试卷已存在");
            return studentService.getExistPaper(paperId);
        }
        //试卷未出好则出卷
        else {
            String studentId="";
            for(Cookie i:cookies){
                if(i.getName().equals("username")) studentId=i.getValue();
            }
            ExamPaper examPaper= studentService.getExamPaper(studentId);
            System.out.println("考卷"+examPaper);
            Cookie cookie=new Cookie("paperId",String.valueOf(examPaper.getPaperId()));
            cookie.setPath("/");
            response.addCookie(cookie);
            return examPaper;
        }


    }

    //提交试卷
    @RequestMapping("/submitPaper")
    @ResponseBody
    public List<List<Integer>> submitPaper(HttpServletRequest request,@RequestBody List<List<Integer>> stuAnswer){
        String studentId="";
        Cookie[] cookies=request.getCookies();
        for(Cookie i:cookies){
            if(i.getName().equals("username")) studentId=i.getValue();
        }
        Integer classId=studentService.getClassId(studentId);
        Integer paperId=0;
        for(Cookie i:cookies){
            if(i.getName().equals("paperId")) paperId=Integer.valueOf(i.getValue());
        }
        ClassExam classExam=studentService.getClassExam(classId);
        if(classExam.getClassStatus()==2)//正式考
        {
            studentService.setScore(paperId,studentId,stuAnswer);
            return studentService.getAnswerList(paperId);
        }
        else//模拟考，返回答案
            return studentService.getAnswerList(paperId);
    }
    @RequestMapping("/getStudentInfo")
    @ResponseBody
    public List<StudentInfo> getStudentInfo(HttpServletRequest request,@RequestBody JSONObject jsonObject){
        Cookie[] cookies=request.getCookies();
        int flag = -1;
        String username="";
        for(Cookie i:cookies){
            if(i.getName().equals("username")){
                username=i.getValue();
            }
            if(i.getName().equals("character")&&i.getValue().equals("admin")){
                flag=0;
            }
            else if(i.getName().equals("character")&&i.getValue().equals("teacher")){
                flag=1;
            }
        }
        String classId=jsonObject.getString("classId");
        return teacherService.getStudentInfo(flag,username,classId);
    }

    @RequestMapping("/getTeacherList")
    @ResponseBody
    public List<TTable> getTeacherInfo(){
        return teacherService.getTeacherList();
    }
    @RequestMapping("/deleteTeacher")
    @ResponseBody
    public Map<String,Integer>deleteTeacher(@RequestBody List<String>teacherId){
        Map<String,Integer>result=new HashMap<>();
        adminService.deleteTeacher(teacherId);
        result.put("status",1);
        return result;
    }
    @RequestMapping("/deleteClass")
    @ResponseBody
    public Map<String,Integer>deleteClass(@RequestBody List<String>classId){
        List<Integer> classIdList=new ArrayList<>();
        Map<String,Integer>result=new HashMap<>();
        for(int i=0;i<classId.size();i++){
            Integer id=Integer.valueOf(classId.get(i));
            classIdList.add(id);
        }
        teacherService.deleteClass(classIdList);
        result.put("status",1);
        return result;
    }

}
