  import { Component } from '@angular/core';
  import { Router, RouterLink } from '@angular/router';
  import { FormsModule } from '@angular/forms';
  import { AuthService } from '../../services/auth.service';

  @Component({
    selector: 'app-login',
    imports: [RouterLink, FormsModule],
    templateUrl: './login.html',
    styleUrls: ['./login.css']
  })
  export class LoginComponent {
    email = '';
    password = '';
    error = '';

    constructor(private authService: AuthService, private router: Router) {}

    login() {
      this.authService.login(this.email, this.password).subscribe({
        next: () => this.router.navigate(['/dashboard']),
        error: (err) => this.error = err.error || 'Erreur de connexion'
      });
    }
  }