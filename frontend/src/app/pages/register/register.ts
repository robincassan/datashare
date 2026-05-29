  import { Component } from '@angular/core';
  import { Router, RouterLink } from '@angular/router';
  import { FormsModule } from '@angular/forms';
  import { AuthService } from '../../services/auth.service';

  @Component({
    selector: 'app-register',
    imports: [RouterLink, FormsModule],
    templateUrl: './register.html',
    styleUrls: ['./register.css']
  })
  export class RegisterComponent {
    email = '';
    password = '';
    confirmPassword = '';
    error = '';

    constructor(private authService: AuthService, private router: Router) {}

    register() {
      if (this.password !== this.confirmPassword) {
        this.error = 'Les mots de passe ne correspondent pas';
        return;
      }
      this.authService.register(this.email, this.password).subscribe({
        next: () => this.router.navigate(['/login']),
        error: (err) => this.error = err.error || "Erreur lors de l'inscription"
      });
    }
  }