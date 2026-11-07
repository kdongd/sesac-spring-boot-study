package com.example.sesac_spring_boot_study.controller;

import com.example.sesac_spring_boot_study.dto.StudyRequest;
import com.example.sesac_spring_boot_study.service.StudyService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/study")
public class StudyController {

    private final StudyService studyService;


    public StudyController(StudyService studyService) {
        this.studyService = studyService;
    }

    // 참가 신청
    @PostMapping("/apply")
    public String apply(@RequestBody StudyRequest request) {
        return studyService.apply(request.name());
    }

    // 참가 취소
    @PostMapping("/cancel/{id}")
    public String cancel(@PathVariable Long id) {
        return studyService.cancel(id);
    }

    // 참가자 명단 조회
    @GetMapping("/participants")
    public String getMembers() {
        return studyService.getMembers();
    }
}
