import { Component, input } from '@angular/core';

type SudoButtonType = 'button' | 'submit' | 'reset';
type SudoButtonVariant = 'primary' | 'secondary' | 'integration';
type SudoButtonSize = 'default' | 'large';

@Component({
  selector: 'sudo-button',
  templateUrl: './sudo-button.component.html',
  styleUrl: './sudo-button.component.scss',
})
export class SudoButtonComponent {

  readonly type = input<SudoButtonType>('button');
  readonly variant = input<SudoButtonVariant>('secondary');
  readonly size = input<SudoButtonSize>('default');
  readonly disabled = input(false);
  readonly busy = input(false);
}
