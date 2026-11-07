package com.example.sesac_spring_boot_study.service;


import com.example.sesac_spring_boot_study.domain.StudyMember;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class StudyService {

    private final List<StudyMember> members = new ArrayList<>();

    private long maxMember = 3;
    private long nextId = 1;

    // 참가 신청
    public String apply(String name) {

        if (name == null || name.trim().isEmpty()) {
            return "이름을 입력해주세요.";
        }

        name = name.trim();

        // 중복 이름 확인
        for (StudyMember member : members) {
            if (member.getName().equals(name)) {
                return "이미 신청한 이름입니다.";
            }
        }

        // 확정 인원 확인
        int count = 0;

        for (StudyMember member : members) {
            if (member.getStatus().equals("CONFIRMED")) {
                count++;
            }
        }

        String status = "WAITING";

        if (count < maxMember) {
            status = "CONFIRMED";
        }

        StudyMember member = new StudyMember(nextId, name, status);

        members.add(member);
        nextId++;

        return "신청 완료 - ID: " + member.getId() + ", 이름: " + member.getName() + ", 상태: " + member.getStatus();
    }

    // 참가 취소
    public String cancel(Long id) {

        StudyMember cancelMember = null;

        for (StudyMember member : members) {
            if (member.getId().equals(id)) {
                cancelMember = member;
                break;
            }
        }

        if (cancelMember == null) {
            return "해당 참가자가 없습니다.";
        }

        String status = cancelMember.getStatus();

        members.remove(cancelMember);

        // 확정자가 취소하면 대기자 한 명을 확정으로 변경
        if (status.equals("CONFIRMED")) {
            for (StudyMember member : members) {
                if (member.getStatus().equals("WAITING")) {
                    member.setStatus("CONFIRMED");
                    break;
                }
            }
        }

        return "참가 신청이 취소되었습니다.";
    }

    // 참가자 목록 조회
    public String getMembers() {

        if (members.isEmpty()) {
            return "등록된 참가자가 없습니다.";
        }

        String result = "";

        for (StudyMember member : members) {
            result += "ID: " + member.getId()
                    + ", 이름: " + member.getName()
                    + ", 상태: " + member.getStatus()
                    + "\n";
        }

        return result;
    }
}


