import { NgModule } from '@angular/core';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { BrowserModule } from '@angular/platform-browser';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatToolbarModule } from '@angular/material/toolbar';

import { AppComponent } from './app.component';
import { AppRoutingModule } from './app-routing.module';
import { ConfirmationDialog } from './dialogs/confirmation/confirmation-dialog.component';
import { HeaderComponent } from './header/header.component';
import { FooterComponent } from './footer/footer.component';
import { HomeComponent } from './pages/home/home.component';
import { LoginComponent } from './pages/login/login.component';
import { MessageDialog } from './dialogs/message/message-dialog.component';
import { SimpleTextComponent } from './text/simple/simple-text.component';
import { EnvironmentComponent } from './pages/environment/environment.component';
import { HttpClientModule } from '@angular/common/http';
import { SandboxComponent } from './pages/sandbox/sandbox.component';
import { ScenarioComponent } from './pages/scenario/scenario.component';

@NgModule({
  declarations: [
    AppComponent,
    ConfirmationDialog,
    EnvironmentComponent,
    FooterComponent,
    HeaderComponent,
    HomeComponent,
    LoginComponent,
    MessageDialog,
    SandboxComponent,
    ScenarioComponent,
    SimpleTextComponent,
  ],
  imports: [
    BrowserAnimationsModule,
    BrowserModule,
    FormsModule,
    HttpClientModule,
    MatButtonModule,
    MatCardModule,
    MatDialogModule,
    MatIconModule,
    MatInputModule,
    MatMenuModule,
    MatProgressSpinnerModule,
    MatToolbarModule,
    ReactiveFormsModule,
    // import as last module!
    AppRoutingModule,
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
