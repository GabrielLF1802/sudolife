package com.sudolife.application.service.user.ports.provided;

import com.sudolife.application.service.user.DeleteAccountCommand;

public interface DeleteAccountUseCase {

    void execute(DeleteAccountCommand command);
}
