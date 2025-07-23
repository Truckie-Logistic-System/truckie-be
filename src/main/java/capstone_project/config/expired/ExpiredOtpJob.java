package capstone_project.config.expired;

import capstone_project.service.services.email.EmailProtocolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExpiredOtpJob implements Job {

    private EmailProtocolService emailProtocolService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
        String email = jobDataMap.getString("email");
        String otp = jobDataMap.getString("otp");

        // Remove the OTP from storage
        emailProtocolService = (EmailProtocolService) ApplicationContextProvider.getApplicationContext()
                .getBean("emailProtocolServiceImpl");

        emailProtocolService.removeOtpIfExpired(email, otp);

        log.info("OTP for email {} has expired and been removed.", email);
    }
}
